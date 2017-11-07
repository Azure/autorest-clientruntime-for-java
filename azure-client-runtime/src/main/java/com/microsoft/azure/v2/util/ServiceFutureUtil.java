/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.v2.util;

import com.microsoft.azure.v2.OperationStatus;
import com.microsoft.rest.v2.ServiceCallback;
import com.microsoft.rest.v2.ServiceFuture;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;

/**
 * Azure-specific helper methods for creating ServiceFuture instances.
 */
public class ServiceFutureUtil {
    /**
     * Creates a ServiceFuture from an observable representing a long-running operation.
     * @param observable The observable representing the long-running operation.
     * @param callback The callback to run when the service future is completed.
     * @param <T> The type of entity expected to be returned from the long-running operation.
     * @return A ServiceFuture representing the long-running operation.
     */
    public static <T> ServiceFuture<T> fromLRO(Observable<OperationStatus<T>> observable, ServiceCallback<T> callback) {
        Single<T> single = observable.lastOrError().map(new Function<OperationStatus<T>, T>() {
            @Override
            public T apply(OperationStatus<T> operationStatus) {
                return operationStatus.result();
            }
        });

        return ServiceFuture.fromBody(single, callback);
    }
}
