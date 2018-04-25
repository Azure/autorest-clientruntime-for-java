/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.arm.resources.collection.implementation;

import com.microsoft.azure.Resource;
import com.microsoft.azure.arm.resources.ResourceId;
import com.microsoft.azure.arm.resources.ResourceUtilsCore;
import com.microsoft.azure.arm.resources.collection.SupportsDeletingByResourceGroup;
import com.microsoft.azure.arm.resources.collection.SupportsGettingById;
import com.microsoft.azure.arm.resources.collection.SupportsGettingByResourceGroup;
import com.microsoft.azure.arm.resources.implementation.ManagerBaseCore;
import com.microsoft.azure.arm.resources.models.GroupableResourceCore;
import com.microsoft.azure.arm.resources.models.HasManager;
import com.microsoft.azure.arm.model.HasInner;
import com.microsoft.azure.arm.utils.SdkContext;
import com.microsoft.rest.ServiceCallback;
import com.microsoft.rest.ServiceFuture;
import rx.Completable;
import rx.Observable;
import rx.functions.Func1;

/**
 * Base class for resource collection classes.
 * (Internal use only)
 * @param <T> the individual resource type returned
 * @param <ImplT> the individual resource implementation
 * @param <InnerT> the wrapper inner type
 * @param <InnerCollectionT> the inner type of the collection object
 * @param <ManagerT> the manager type for this resource provider type
 */
public abstract class GroupableResourcesCoreImpl<
        T extends GroupableResourceCore<ManagerT, InnerT>,
        ImplT extends T,
        InnerT extends Resource,
        InnerCollectionT,
        ManagerT extends ManagerBaseCore>
    extends CreatableResourcesImpl<T, ImplT, InnerT>
    implements
        SupportsGettingById<T>,
        SupportsGettingByResourceGroup<T>,
        SupportsDeletingByResourceGroup,
        HasManager<ManagerT>,
        HasInner<InnerCollectionT> {

    private final InnerCollectionT innerCollection;
    private final ManagerT myManager;
    protected GroupableResourcesCoreImpl(
            InnerCollectionT innerCollection,
            ManagerT manager) {
        this.innerCollection = innerCollection;
        this.myManager = manager;
    }

    @Override
    public InnerCollectionT inner() {
        return this.innerCollection;
    }

    @Override
    public ManagerT manager() {
        return this.myManager;
    }

    @Override
    public T getById(String id) {
        return getByIdAsync(id).toBlocking().last();
    }

    @Override
    public final Observable<T> getByIdAsync(String id) {
        ResourceId resourceId = ResourceId.fromString(id);

        if (resourceId == null) {
            return null;
        }

        return getByResourceGroupAsync(resourceId.resourceGroupName(), resourceId.name());
    }

    @Override
    public final ServiceFuture<T> getByIdAsync(String id, ServiceCallback<T> callback) {
        return ServiceFuture.fromBody(getByIdAsync(id), callback);
    }

    @Override
    public final void deleteByResourceGroup(String groupName, String name) {
        deleteByResourceGroupAsync(groupName, name).await();
    }

    @Override
    public final ServiceFuture<Void> deleteByResourceGroupAsync(String groupName, String name, ServiceCallback<Void> callback) {
        return ServiceFuture.fromBody(deleteByResourceGroupAsync(groupName, name).andThen(Observable.<Void>just(null)), callback);
    }

    @Override
    public Completable deleteByResourceGroupAsync(String groupName, String name) {
        return this.deleteInnerAsync(groupName, name).subscribeOn(SdkContext.getRxScheduler());
    }

    @Override
    public Completable deleteByIdAsync(String id) {
        return deleteByResourceGroupAsync(ResourceUtilsCore.groupFromResourceId(id), ResourceUtilsCore.nameFromResourceId(id));
    }

    @Override
    public T getByResourceGroup(String resourceGroupName, String name) {
        return getByResourceGroupAsync(resourceGroupName, name).toBlocking().last();
    }

    @Override
    public Observable<T> getByResourceGroupAsync(String resourceGroupName, String name) {
        return this.getInnerAsync(resourceGroupName, name).map(new Func1<InnerT, T>() {
            @Override
            public T call(InnerT innerT) {
                return wrapModel(innerT);
            }
        });
    }

    @Override
    public ServiceFuture<T> getByResourceGroupAsync(String resourceGroupName, String name, ServiceCallback<T> callback) {
        return ServiceFuture.fromBody(getByResourceGroupAsync(resourceGroupName, name), callback);
    }

    protected abstract Observable<InnerT> getInnerAsync(String resourceGroupName, String name);

    protected abstract Completable deleteInnerAsync(String resourceGroupName, String name);
}
