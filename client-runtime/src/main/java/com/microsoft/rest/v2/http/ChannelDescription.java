/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.rest.v2.http;


import io.netty.channel.Channel;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

class ChannelDescription {
    Channel channel;
    ZonedDateTime availableSince;

    ChannelDescription(Channel channel) {
        this.channel = channel;
    }

    long idleDurationInSec() {
        long idleDurationInSec = ChronoUnit.SECONDS.between(availableSince, ZonedDateTime.now(ZoneOffset.UTC));
        return idleDurationInSec;
    }

    String id() {
        return channel.id().asLongText();
    }

    @Override
    public String toString() {
        return String.format("Id:%s IdleDurationInSec:%s", id(), idleDurationInSec());
    }
}
