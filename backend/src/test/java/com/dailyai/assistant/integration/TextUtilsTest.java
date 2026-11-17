package com.dailyai.assistant.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextUtilsTest {

    @Test
    void removesTagsScriptsAndDecodesEntities() {
        String html = "<script>alert('x')</script><p>Hello &amp; welcome&nbsp;to AI</p><style>p{}</style>";
        assertThat(TextUtils.cleanHtml(html)).isEqualTo("Hello & welcome to AI");
    }

    @Test
    void decodesNumericEntitiesAndCollapsesWhitespace() {
        assertThat(TextUtils.cleanHtml("A&#8217;s   <b>test</b>\n")).isEqualTo("A\u2019s test");
    }

    @Test
    void truncatesSafely() {
        assertThat(TextUtils.truncate("abcdef", 4)).isEqualTo("abcd");
        assertThat(TextUtils.truncate(null, 4)).isNull();
    }
}
