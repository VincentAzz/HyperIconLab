/**
 * OpenMP stub symbols for opencv-mobile compatibility.
 * The opencv-mobile library was compiled with GCC's -static-openmp.
 * libkleidicv_thread.a provides most OpenMP stubs for ARM paths,
 * but opencv_core's parallel.cpp uses __kmpc_dispatch_* which are not stubbed.
 * These stubs fill the gap for the non-ARM-optimized parallel_for path.
 *
 * NOTE: We only stub symbols that are actually undefined. Symbols already
 * provided by libkleidicv_thread.a must NOT be stubbed to avoid duplicates.
 */

extern "C" {

void __kmpc_dispatch_deinit(void *loc, int tid) {
    (void) loc;
    (void) tid;
}

void __kmpc_dispatch_init(void *loc, int tid) {
    (void) loc;
    (void) tid;
}

} // extern "C"