package com.microsoft.rest.v2;

import com.microsoft.rest.v2.annotations.GET;
import com.microsoft.rest.v2.annotations.Host;
import com.microsoft.rest.v2.annotations.PathParam;

/**
 * Created by jianghlu on 8/7/17.
 */
public class GetSimple {

    @Host("http://httpbin.org")
    public interface GetSimpleService {
        @GET("bytes/{bytes}")
        byte[] getBytes(@PathParam("bytes") int bytes);
    }
}
