package com.webfunny.android.sample;

import com.tencent.mmkv.MMKV;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 该类包含用户接入过程中主要的需要配置的参数。
 */
public class WebFunnyCustomConfig {
    public static final String ENDPOINT = "http://test.webfunny.cn/wfMonitor/otel/trace";
    public static final String APPLICATION_NAME = "WebFunnyDemoApp";
    public static final String DEPLOYMENT_ENV = "dev";

    public static final String CUSTOM_GLOBAL_ATTRS_VENDOR = "Webfunny";
    public static final String CUSTOM_GLOBAL_ATTRS_USER_ID = "11120240001";
    public static final String CUSTOM_GLOBAL_ATTRS_USER_TAG = "newUser";
    public static final String CUSTOM_GLOBAL_ATTRS_PROJECT_VER = "111.111";
    public static final String CUSTOM_GLOBAL_ATTRS_CUSTOM = "11120240001";

    private static final String MMKV_KEY_UUID = "customUuid";
    private static String uuid;

    /**
     * 生成UUID，每次安装只会生成一次。清空数据或者重新安装后，该值会变。
     * @return uuid
     */
    public static String getUUID() {
        if (uuid == null || uuid.isEmpty()) {
            uuid = MMKV.defaultMMKV().getString(MMKV_KEY_UUID,"");
        }
        if (uuid.isEmpty()) {
            String seed = CUSTOM_GLOBAL_ATTRS_USER_ID + System.currentTimeMillis();
            uuid = UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
            MMKV.defaultMMKV().putString(MMKV_KEY_UUID, uuid);
        }
        return uuid;
    }
}
