#include <jni.h>
#include <dlfcn.h>
#include <stdio.h>
#include <stddef.h>
#include <malloc.h>
#include <sys/prctl.h>

#ifndef PR_SET_TAGGED_ADDR_CTRL
#define PR_SET_TAGGED_ADDR_CTRL 55
#endif

#ifndef M_BIONIC_SET_HEAP_TAGGING_LEVEL
#define M_BIONIC_SET_HEAP_TAGGING_LEVEL (-204)
#endif

#ifndef M_HEAP_TAGGING_LEVEL_NONE
#define M_HEAP_TAGGING_LEVEL_NONE 0
#endif

#define OPT_SET_HEAP_TAGGING_LEVEL 8

typedef _Bool (*android_mallopt_t)(int, void*, size_t);

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM* vm, char* options, void* reserved) {
    // 1. Вимикаємо тегування вказівок на рівні ядра Linux через prctl
    int prctl_res = prctl(PR_SET_TAGGED_ADDR_CTRL, 0, 0, 0, 0);
    fprintf(stderr, "[notbi] prctl PR_SET_TAGGED_ADDR_CTRL(0): %d\n", prctl_res);

    // 2. Вимикаємо через публічний Bionic mallopt
    int mallopt_res = mallopt(M_BIONIC_SET_HEAP_TAGGING_LEVEL, M_HEAP_TAGGING_LEVEL_NONE);
    fprintf(stderr, "[notbi] mallopt M_BIONIC_SET_HEAP_TAGGING_LEVEL: %d\n", mallopt_res);

    // 3. Вимикаємо через внутрішній android_mallopt
    android_mallopt_t fn = (android_mallopt_t)dlsym(RTLD_DEFAULT, "android_mallopt");
    if (fn) {
        int level = M_HEAP_TAGGING_LEVEL_NONE;
        _Bool ok = fn(OPT_SET_HEAP_TAGGING_LEVEL, &level, sizeof(level));
        fprintf(stderr, "[notbi] android_mallopt: %s\n", ok ? "ok" : "FAILED");
    }

    fflush(stderr);
    return JNI_OK;
}
