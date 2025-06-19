package com.kakaobase.snsapp.global.common.redis.service;

//
public interface CacheSyncService<V> {
    //동기화가 필요한 키값 목록 반환
    void syncCacheToDB();

    void addToSyncList(V value);

    void removeFromSyncList(V value);
}
