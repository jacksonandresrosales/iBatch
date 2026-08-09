package com.iroute.ibatch.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    @Test
    void shouldShareReprocessLimitAcrossTransactionIds() throws Exception {
        var filter = new RateLimitFilter();

        for (var transactionId = 1; transactionId <= 20; transactionId++) {
            var response = filter(filter, "/transactions/" + transactionId);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        var response = filter(filter, "/transactions/21");

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
    }

    private MockHttpServletResponse filter(RateLimitFilter filter, String uri) throws Exception {
        var request = new MockHttpServletRequest("POST", uri);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
