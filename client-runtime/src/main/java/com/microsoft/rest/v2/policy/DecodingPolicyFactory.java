package com.microsoft.rest.v2.policy;

import com.microsoft.rest.v2.http.HttpRequest;
import com.microsoft.rest.v2.http.HttpResponse;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;

/**
 * Creates a RequestPolicy which decodes the response body and headers.
 */
public class DecodingPolicyFactory implements RequestPolicyFactory {
    @Override
    public RequestPolicy create(RequestPolicy next, RequestPolicyOptions options) {
        return new DecodingPolicy(next);
    }

    private class DecodingPolicy implements RequestPolicy {
        private final RequestPolicy next;
        private DecodingPolicy(RequestPolicy next) {
            this.next = next;
        }

        @Override
        public Single<HttpResponse> sendAsync(final HttpRequest request) {
            return next.sendAsync(request).flatMap(new Function<HttpResponse, SingleSource<? extends HttpResponse>>() {
                @Override
                public SingleSource<? extends HttpResponse> apply(final HttpResponse response) throws Exception {
                    return request.responseDecoder().decode(response);
                }
            });
        }
    }
}
