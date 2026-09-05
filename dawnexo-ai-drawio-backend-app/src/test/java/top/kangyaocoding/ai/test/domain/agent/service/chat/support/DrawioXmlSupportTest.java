package top.kangyaocoding.ai.test.domain.agent.service.chat.support;

import org.junit.jupiter.api.Test;
import top.kangyaocoding.ai.domain.agent.service.chat.support.DrawioXmlSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @description: DrawioXmlSupport 单元测试：覆盖合法 XML、Markdown 围栏、JSON 契约、
 * 转义污染、截断残缺、空内容六类场景。
 * @author: herbert
 * @date: 2026-09-04
 */
public class DrawioXmlSupportTest {

    /** 含两个内容单元格（除 id=0/1）的最小合法图表 */
    private static final String MINIMAL_XML = "<mxGraphModel dx=\"800\" dy=\"600\" grid=\"1\" page=\"1\">"
            + "<root>"
            + "<mxCell id=\"0\"/>"
            + "<mxCell id=\"1\" parent=\"0\"/>"
            + "<mxCell id=\"2\" value=\"开始\" style=\"rounded=1\" vertex=\"1\" parent=\"1\"><mxGeometry x=\"100\" y=\"100\" width=\"120\" height=\"60\" as=\"geometry\"/></mxCell>"
            + "<mxCell id=\"3\" value=\"结束\" style=\"rounded=1\" vertex=\"1\" parent=\"1\"><mxGeometry x=\"300\" y=\"100\" width=\"120\" height=\"60\" as=\"geometry\"/></mxCell>"
            + "</root></mxGraphModel>";

    @Test
    public void extract_and_validate_plain_xml() {
        String extracted = DrawioXmlSupport.extractDrawioXml("说明文字\n" + MINIMAL_XML + "\n说明文字");
        assertEquals(MINIMAL_XML, extracted);
        assertTrue(DrawioXmlSupport.isValidDrawioXml(extracted));
    }

    @Test
    public void extract_from_markdown_fence() {
        String wrapped = "```xml\n" + MINIMAL_XML + "\n```";
        assertEquals(MINIMAL_XML, DrawioXmlSupport.extractDrawioXml(wrapped));
        assertTrue(DrawioXmlSupport.isValidDrawioXml(DrawioXmlSupport.extractDrawioXml(wrapped)));
    }

    @Test
    public void extract_last_block_when_multiple() {
        String another = MINIMAL_XML.replace("x=\"100\"", "x=\"150\"");
        String text = MINIMAL_XML + "\n中间文字\n" + another;
        assertEquals(another, DrawioXmlSupport.extractDrawioXml(text));
    }

    @Test
    public void extract_from_contract_json() {
        // 三字段契约：diagram 字段内是带真实换行的 XML（JSON 字符串化后为 \n 字面序列）
        String json = "{\"type\":\"mixed\",\"explanation\":\"已生成图表\",\"diagram\":\"" + MINIMAL_XML.replace("\"", "\\\"") + "\"}";
        String extracted = DrawioXmlSupport.extractDrawioXml(json);
        // 转义后的副本（含 \" 序列）会被判定为 JSON 污染而过滤，这里整体非 "<" 开头，应返回 null
        assertNull(extracted);
    }

    @Test
    public void truncated_xml_is_invalid() {
        String truncated = MINIMAL_XML.substring(0, MINIMAL_XML.length() / 2);
        String extracted = DrawioXmlSupport.extractDrawioXml(truncated);
        // 截断文本以 "<" 开头会原样返回，但 DOM 校验必须不通过
        assertFalse(DrawioXmlSupport.isValidDrawioXml(extracted));
    }

    @Test
    public void xml_without_content_cells_is_invalid() {
        String empty = "<mxGraphModel><root>"
                + "<mxCell id=\"0\"/><mxCell id=\"1\" parent=\"0\"/>"
                + "</root></mxGraphModel>";
        // 只有画布根单元格，无实际内容，判为无效
        assertFalse(DrawioXmlSupport.isValidDrawioXml(empty));
    }

    @Test
    public void null_and_blank_inputs() {
        assertNull(DrawioXmlSupport.extractDrawioXml(null));
        assertNull(DrawioXmlSupport.extractDrawioXml(""));
        assertNull(DrawioXmlSupport.extractDrawioXml("纯文本，没有 XML"));
        assertFalse(DrawioXmlSupport.isValidDrawioXml(null));
        assertFalse(DrawioXmlSupport.isValidDrawioXml(""));
    }

    @Test
    public void xxe_payload_is_rejected() {
        String xxe = "<mxGraphModel><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                + "<root><mxCell id=\"0\"/><mxCell id=\"2\" value=\"&xxe;\"/></root></mxGraphModel>";
        assertFalse(DrawioXmlSupport.isValidDrawioXml(xxe));
    }
}
