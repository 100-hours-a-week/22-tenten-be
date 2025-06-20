package com.kakaobase.snsapp.global.common.redis.service;

import java.util.List;


public interface CacheSyncService<V> { //V는 동기화 목록 Set에 들어갈 값

    //동기화가 필요한 키값 목록 반환
    void syncCacheToDB();
    //동기화목록에 값 추가
    void addToSyncList(V value);
    //동기화 목록 단일 제거
    void removeFromSyncList(V value);
    //동기화 목록 단체 제거
    void removeFromSyncList(List<V> keys);
}
