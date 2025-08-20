#include "MMKV/MMKV.h"

using namespace std;
using namespace mmkv;

typedef void (Logger)(int, const char *, const char *);

static Logger *g_logger = nullptr;

static void LogCallback(const MMKVLogLevel level,
                        const char *file,
                        int line,
                        const char *function,
                        MMKVLog_t message) {
    if (g_logger) {
        g_logger(level, file, message.c_str());
    }
}

static bool isNotNullOrEmpty(const char *str) {
    return str != nullptr && strlen(str) > 0;
}

// 工具：将 string 拷贝为以 '\0' 结尾的 C 字符串（由调用方负责释放）
static char *stringToChar(const string &src) {
    const auto len = src.size() + 1;
    const auto buf = static_cast<char *>(malloc(len));
    if (buf == nullptr) return nullptr;
    memcpy(buf, src.c_str(), len);
    return buf;
}

extern "C" void mmkv_initialize(const char *path, int level, Logger *logger) {
    g_logger = logger;
    MMKV::initializeMMKV(path, static_cast<MMKVLogLevel>(level), LogCallback);
}


extern "C" MMKV *mmkv_defaultMMKV(int mode, const char *cryptKey) {
    MMKV *mmkv = nullptr;
    if (isNotNullOrEmpty(cryptKey)) {
        const string crypt(cryptKey);
        mmkv = MMKV::defaultMMKV(static_cast<MMKVMode>(mode), &crypt);
    } else {
        mmkv = MMKV::defaultMMKV(static_cast<MMKVMode>(mode));
    }
    mmkv->enableAutoKeyExpire(MMKV::ExpireNever);
    return mmkv;
}

extern "C" MMKV *mmkv_mmkvWithID(const char *id, int mode, const char *cryptKey, const char *path) {
    MMKV *mmkv = nullptr;
    if (isNotNullOrEmpty(cryptKey) && isNotNullOrEmpty(path)) {
        const string crypt(cryptKey);
        const string rootPath(path);
        mmkv = MMKV::mmkvWithID(id, static_cast<MMKVMode>(mode), &crypt, &rootPath);
    } else if (isNotNullOrEmpty(cryptKey) && path == nullptr) {
        const string crypt(cryptKey);
        mmkv = MMKV::mmkvWithID(id, static_cast<MMKVMode>(mode), &crypt);
    } else if (isNotNullOrEmpty(path) && cryptKey == nullptr) {
        const MMKVPath_t rootPath(path);
        mmkv = MMKV::mmkvWithID(id, static_cast<MMKVMode>(mode), nullptr, &rootPath);
    } else {
        mmkv = MMKV::mmkvWithID(id, static_cast<MMKVMode>(mode));
    }
    mmkv->enableAutoKeyExpire(MMKV::ExpireNever);
    return mmkv;
}

extern "C" int getInt(MMKV *mmkv, const char *key, const int defaultValue) {
    return mmkv->getInt32(key, defaultValue);
}

extern "C" bool setInt(MMKV *mmkv, const char *key, const int value) {
    return mmkv->set(value, key);
}

// String
extern "C" const char *getString(MMKV *mmkv, const char *key, const char *defaultValue) {
    if (string tmp; mmkv->getString(key, tmp)) {
        return stringToChar(tmp);
    }
    return stringToChar(string(defaultValue));
}

extern "C" bool setString(MMKV *mmkv, const char *key, const char *value) {
    return mmkv->set(string(value), key);
}

// Float
extern "C" float getFloat(MMKV *mmkv, const char *key, const float defaultValue) {
    return mmkv->getFloat(key, defaultValue);
}

extern "C" bool setFloat(MMKV *mmkv, const char *key, const float value) {
    return mmkv->set(value, key);
}

// Long (使用 int64_t 表达 64 位整数)
extern "C" int64_t getLong(MMKV *mmkv, const char *key, const int64_t defaultValue) {
    return mmkv->getInt64(key, defaultValue);
}

extern "C" bool setLong(MMKV *mmkv, const char *key, const int64_t value) {
    return mmkv->set(value, key);
}

// Double
extern "C" double getDouble(MMKV *mmkv, const char *key, const double defaultValue) {
    return mmkv->getDouble(key, defaultValue);
}

extern "C" bool setDouble(MMKV *mmkv, const char *key, const double value) {
    return mmkv->set(value, key);
}

// Boolean
extern "C" bool getBoolean(MMKV *mmkv, const char *key, const bool defaultValue) {
    return mmkv->getBool(key, defaultValue);
}

extern "C" bool setBoolean(MMKV *mmkv, const char *key, const bool value) {
    return mmkv->set(value, key);
}

// ByteArray
extern "C" uint8_t *getByteArray(MMKV *mmkv, const char *key, size_t *size) {
    if (MMBuffer buffer; mmkv->getBytes(key, buffer)) {
        *size = buffer.length();
        const auto data = static_cast<uint8_t *>(malloc(*size));
        if (data == nullptr) {
            return nullptr; // 内存分配失败
        }
        memcpy(data, buffer.getPtr(), *size);
        return data;
    }
    return nullptr;
}

extern "C" bool setByteArray(MMKV *mmkv, const char *key, uint8_t *value, const size_t size) {
    const auto buffer = MMBuffer(value, size, MMBufferNoCopy);
    return mmkv->set(buffer, key);
}

// StringList
struct StringListReturn {
    char **items;
    size_t size;
};

// UInt
extern "C" uint32_t getUInt(MMKV *mmkv, const char *key, const uint32_t defaultValue) {
    return mmkv->getUInt32(key, defaultValue);
}

extern "C" bool setUInt(MMKV *mmkv, const char *key, const uint32_t value) {
    return mmkv->set(value, key);
}

// ULong
extern "C" uint64_t getULong(MMKV *mmkv, const char *key, const uint64_t defaultValue) {
    return mmkv->getUInt64(key, defaultValue);
}

extern "C" bool setULong(MMKV *mmkv, const char *key, const uint64_t value) {
    return mmkv->set(value, key);
}

extern "C" StringListReturn *getStringSet(MMKV *mmkv, const char *key) {
    if (vector<string> vec; mmkv->getVector(key, vec)) {
        const auto rtn = static_cast<StringListReturn *>(malloc(sizeof(StringListReturn)));
        if (rtn == nullptr) {
            return nullptr;
        }

        rtn->size = vec.size();

        if (rtn->size == 0) {
            rtn->items = nullptr;
            return rtn;
        }

        rtn->items = static_cast<char **>(calloc(rtn->size, sizeof(char *)));
        if (rtn->items == nullptr) {
            free(rtn);
            return nullptr;
        }

        for (size_t i = 0; i < rtn->size; ++i) {
            rtn->items[i] = stringToChar(vec[i]);
        }

        return rtn;
    }
    return nullptr;
}

extern "C" bool setStringSet(MMKV *mmkv, const char *key, const char **value, const size_t size) {
    if (value) {
        vector<string> vec;
        vec.reserve(size);
        for (size_t i = 0; i < size; i++) {
            if (value[i]) {
                vec.emplace_back(value[i]);
            }
        }
        return mmkv->set(vec, key);
    }
    return mmkv->removeValueForKey(key);
}

extern "C" void mmkv_removeValueForKey(MMKV *mmkv, const char *key) {
    mmkv->removeValueForKey(key);
}

extern "C" void mmkv_removeValuesForKeys(MMKV *mmkv, const char **keys, const size_t size) {
    vector<string> vec;
    vec.reserve(size);
    for (size_t i = 0; i < size; ++i) {
        vec.emplace_back(keys[i]);
    }
    mmkv->removeValuesForKeys(vec);
}

extern "C" long mmkv_actualSize(MMKV *mmkv) {
    return mmkv->actualSize();
}

extern "C" long mmkv_count(MMKV *mmkv) {
    return mmkv->count();
}

extern "C" long mmkv_totalSize(MMKV *mmkv) {
    return mmkv->totalSize();
}

extern "C" void mmkv_clearMemoryCache(MMKV *mmkv) {
    mmkv->clearMemoryCache();
}

extern "C" void mmkv_clearAll(MMKV *mmkv) {
    mmkv->clearAll();
}

extern "C" void mmkv_close(MMKV *mmkv) {
    mmkv->close();
}

extern "C" StringListReturn *mmkv_allKeys(MMKV *mmkv) {
    const vector<string> vector = mmkv->allKeys();

    const auto rtn = static_cast<StringListReturn *>(malloc(sizeof(StringListReturn)));

    if (rtn == nullptr) {
        return nullptr;
    }

    rtn->size = 0;
    rtn->items = static_cast<char **>(malloc(sizeof(char **) * vector.size()));
    if (rtn->items == nullptr) {
        return nullptr;
    }
    for (string str: vector) {
        (rtn->items)[rtn->size] = stringToChar(str);
        (rtn->size) += 1;
    }
    return rtn;
}

extern "C" bool mmkv_containsKey(MMKV *mmkv, const char *key) {
    return mmkv->containsKey(key);
}

extern "C" void mmkv_checkReSetCryptKey(MMKV *mmkv, const char *cryptKey) {
    const string crypt(cryptKey);
    mmkv->checkReSetCryptKey(&crypt);
}

extern "C" char *mmkv_mmapID(const MMKV *mmkv) {
    return stringToChar(mmkv->mmapID());
}

extern "C" void mmkv_sync(MMKV *mmkv, bool flag) {
    mmkv->sync(static_cast<SyncFlag>(flag));
}

extern "C" void mmkv_trim(MMKV *mmkv) {
    mmkv->trim();
}

extern "C" bool mmkv_backupOneToDirectory(const char *mmapID, const char *dstDir, const char *srcDir) {
    MMKVPath_t srcPath;
    if (srcDir != nullptr) {
        srcPath = MMKVPath_t(srcDir);
    }
    return MMKV::backupOneToDirectory(mmapID, dstDir, &srcPath);
}

extern "C" long mmkv_pageSize() {
    return DEFAULT_MMAP_SIZE;
}

extern "C" void mmkv_setLogLevel(int level) {
    MMKV::setLogLevel(static_cast<MMKVLogLevel>(level));
}

extern "C" const char *mmkv_version() {
    return MMKV_VERSION;
}

extern "C" void mmkv_unregisterHandler() {
    MMKV::unRegisterLogHandler();
    MMKV::unRegisterErrorHandler();
}
