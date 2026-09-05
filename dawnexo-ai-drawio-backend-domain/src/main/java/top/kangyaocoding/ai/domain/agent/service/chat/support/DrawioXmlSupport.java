package top.kangyaocoding.ai.domain.agent.service.chat.support;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description: draw.io XML 提取与校验工具。
 * 从大模型输出中提取 mxGraphModel/mxfile XML 块，并做结构校验（DOM 解析 + 有效内容检查），
 * 只有校验通过的完整 XML 才允许下发给前端渲染，保证画布加载的一定是可解析的图表。
 * 提取规则与前 dawnexo-ai-drawio-front/app/utils/drawio.ts 的 extractDrawioXml 保持一致，形成双端容错。
 * @author: herbert
 * @date: 2026-09-04
 */
@Slf4j
public final class DrawioXmlSupport {

    /** Markdown 代码块围栏：```xml ... ``` 或 ``` ... ``` */
    private static final Pattern MARKDOWN_FENCE = Pattern.compile("```(?:xml)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    /** mxGraphModel / mxfile 完整 XML 块 */
    private static final Pattern DRAWIO_BLOCK = Pattern.compile("<(mxGraphModel|mxfile)\\b[\\s\\S]*?</\\1>", Pattern.CASE_INSENSITIVE);

    /** JSON 转义污染特征：反斜杠后跟引号或 n（说明该块来自被 JSON 转义过的副本，直接加载会失败） */
    private static final Pattern JSON_ESCAPE_POLLUTION = Pattern.compile("\\\\[\"n]");

    private DrawioXmlSupport() {
    }

    /**
     * 剥离 Markdown 代码块围栏（```xml ... ``` / ``` ... ```），保留围栏内内容。
     * 模型可能无视 prompt 的"禁止代码块"约束把 JSON/XML 包进围栏输出，解析前统一剥壳。
     *
     * @param text 原始文本
     * @return 剥离围栏后的文本（入参为 null 时返回 null）
     */
    public static String stripMarkdownFence(String text) {
        if (text == null) {
            return null;
        }
        return MARKDOWN_FENCE.matcher(text).replaceAll("$1").trim();
    }

    /**
     * 从大模型原始输出中提取 draw.io XML。
     * 依次剥离 Markdown 围栏、抓取所有 XML 块并过滤 JSON 转义污染副本，返回最后一个有效块；无匹配时整体以 "<" 开头则原样返回。
     *
     * @param text 大模型原始输出
     * @return 提取到的 XML 文本，提取失败返回 null
     */
    public static String extractDrawioXml(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        // 剥离 Markdown 围栏
        String stripped = stripMarkdownFence(text);
        if (stripped.isEmpty()) {
            return null;
        }

        String lastValid = null;
        Matcher matcher = DRAWIO_BLOCK.matcher(stripped);
        while (matcher.find()) {
            String candidate = matcher.group();
            if (!JSON_ESCAPE_POLLUTION.matcher(candidate).find()) {
                lastValid = candidate;
            }
        }
        if (lastValid != null) {
            return lastValid;
        }
        // 无完整块时，整体本身像 XML 则原样返回（可能是被截断包围的裸 XML）
        return stripped.startsWith("<") ? stripped : null;
    }

    /**
     * 校验 XML 是否为可渲染的 draw.io 图表：能通过 DOM 解析、根节点为 mxGraphModel/mxfile，且除画布根单元格（id=0/1）外至少存在一个 mxCell。
     *
     * @param xml 待校验的 XML 文本
     * @return true 表示可安全下发给前端画布加载
     */
    public static boolean isValidDrawioXml(String xml) {
        if (xml == null || xml.isEmpty()) {
            return false;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 禁用外部实体与 DTD，防止 XXE
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setNamespaceAware(false);

            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            Element root = document.getDocumentElement();
            if (root == null) {
                return false;
            }
            String rootName = root.getTagName();
            if (!"mxGraphModel".equalsIgnoreCase(rootName) && !"mxfile".equalsIgnoreCase(rootName)) {
                return false;
            }
            return countContentCells(root) > 0;
        } catch (Exception e) {
            log.debug("draw.io XML 校验失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 统计除画布根单元格（id=0/1）外的 mxCell 数量，用于判断图表是否承载了实际内容。
     */
    private static int countContentCells(Element root) {
        NodeList cells = root.getElementsByTagName("mxCell");
        int count = 0;
        for (int i = 0; i < cells.getLength(); i++) {
            Element cell = (Element) cells.item(i);
            String id = cell.getAttribute("id");
            if (!"0".equals(id) && !"1".equals(id)) {
                count++;
            }
        }
        return count;
    }
}
