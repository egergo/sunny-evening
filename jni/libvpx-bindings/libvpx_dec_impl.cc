// Copyright 2012 Google Inc. All Rights Reserved.
// Author: frkoenig@google.com (Fritz Koenig)
#include <jni.h>
#include <string.h>
#include <new>

#include "vpx/vpx_decoder.h"
#include "vpx/vp8dx.h"

#ifdef NDEBUG
# define printf(fmt, ...)
#else
# ifdef __ANDROID__
#  include <android/log.h>
#  define printf(fmt, ...) \
   __android_log_print(ANDROID_LOG_DEBUG, "LIBVPX_DEC", fmt, ##__VA_ARGS__)
# else
#  define printf(fmt, ...) \
   printf(fmt "\n", ##__VA_ARGS__)
# endif
#endif

#define FUNC(RETURN_TYPE, NAME, ...) \
  extern "C" { \
  JNIEXPORT RETURN_TYPE Java_com_google_libvpx_LibVpxDec_ ## NAME \
                      (JNIEnv * env, jobject thiz, ##__VA_ARGS__);\
  } \
  JNIEXPORT RETURN_TYPE Java_com_google_libvpx_LibVpxDec_ ## NAME \
                      (JNIEnv * env, jobject thiz, ##__VA_ARGS__)\

static const struct codec_item {
  const char              *name;
  const vpx_codec_iface_t *iface;
  unsigned int             fourcc;
} codecs[] = {
  {"vp8",  &vpx_codec_vp8_dx_algo, 0x30385056},
};

FUNC(jlong, vpxCodecDecAllocCfg) {
  printf("vpxCodecDecAllocCfg");
  vpx_codec_dec_cfg_t *cfg = new (std::nothrow) vpx_codec_dec_cfg_t;

  if (cfg) {
    memset(cfg, 0, sizeof(*cfg));
  }
  return (intptr_t)cfg;
}

FUNC(void, vpxCodecDecFreeCfg, jlong jcfg) {
  printf("vpxCodecDecFreeCfg");
  const vpx_codec_dec_cfg_t *cfg =
      reinterpret_cast<vpx_codec_dec_cfg_t*>(jcfg);

  delete cfg;
}

FUNC(jlong, vpxCodecDecAllocPostProcCfg) {
  printf("vpxCodecDecAllocPostProcCfg");
  vp8_postproc_cfg_t *cfg = new (std::nothrow) vp8_postproc_cfg_t;

  if (cfg) {
    memset(cfg, 0, sizeof(*cfg));
  }

  return (intptr_t)cfg;
}

FUNC(void, vpxCodecDecFreePostProcCfg, jlong jcfg) {
  printf("vpxCodecDecFreePostProcCfg");
  const vp8_postproc_cfg_t *cfg = reinterpret_cast<vp8_postproc_cfg_t*>(jcfg);

  delete cfg;
}

FUNC(jboolean, vpxCodecDecInit, jlong jctx, jlong jcfg,
                                jboolean jpostproc, jboolean jec_enabled) {
  printf("vpxCodecDecInit");

  vpx_codec_ctx_t *ctx = reinterpret_cast<vpx_codec_ctx_t *>(jctx);
  vpx_codec_dec_cfg_t *cfg = reinterpret_cast<vpx_codec_dec_cfg_t *>(jcfg);
  const struct codec_item *codec = codecs;

  const int postproc = jpostproc ? JNI_TRUE : JNI_FALSE;
  const int ec_enabled = jec_enabled ? JNI_TRUE : JNI_FALSE;

  const int dec_flags = (postproc ? VPX_CODEC_USE_POSTPROC : 0) |
                        (ec_enabled ? VPX_CODEC_USE_ERROR_CONCEALMENT : 0);

  if (vpx_codec_dec_init(ctx, codec->iface, cfg, dec_flags) != VPX_CODEC_OK) {
    return false;
  }

  return true;
}

#define SET_DEC_CTL_PARAM(JNI_NAME, CTL_NAME, TYPE) \
  FUNC(int, vpxCodecDecCtlSet ##JNI_NAME, jlong jctx, jint jparam) { \
    printf("vpxCodecDecCtlSet" #JNI_NAME); \
    printf("Setting control parameter " #CTL_NAME " to %d", jparam); \
    vpx_codec_ctx_t *ctx = reinterpret_cast<vpx_codec_ctx_t *>(jctx); \
    return vpx_codec_control(ctx, VP8_ ##CTL_NAME, (TYPE)jparam); \
  }

SET_DEC_CTL_PARAM(SetReference, SET_REFERENCE, vpx_ref_frame_t *)
SET_DEC_CTL_PARAM(CopyReference, COPY_REFERENCE, vpx_ref_frame_t *)
SET_DEC_CTL_PARAM(PostProc, SET_POSTPROC, vp8_postproc_cfg_t *)
SET_DEC_CTL_PARAM(DbgColorRefFrame, SET_DBG_COLOR_REF_FRAME, int)
SET_DEC_CTL_PARAM(DbgColorMBModes, SET_DBG_COLOR_MB_MODES, int)
SET_DEC_CTL_PARAM(DbgColorBModes, SET_DBG_COLOR_B_MODES, int)
SET_DEC_CTL_PARAM(DbgDisplayMV, SET_DBG_DISPLAY_MV, int)

#define POSTPROC_FIELD(JNI_NAME, FIELD_NAME, TYPE) \
  FUNC(void, vpxCodecDecSet ##JNI_NAME, jlong jcfg, jint jparam) { \
    printf("vpxCodecDecSet" #JNI_NAME); \
    printf("Setting cfg->" #FIELD_NAME " = %d", jparam); \
    vp8_postproc_cfg_t *cfg = reinterpret_cast<vp8_postproc_cfg_t*>(jcfg); \
    cfg->FIELD_NAME = (TYPE)jparam;\
  } \
  FUNC(int, vpxCodecDecGet ##JNI_NAME, jlong jcfg) { \
    printf("vpxCodecDecGet" #JNI_NAME); \
    const vp8_postproc_cfg_t *cfg = \
        reinterpret_cast<vp8_postproc_cfg_t*>(jcfg); \
    printf("Getting cfg->" #FIELD_NAME " = %d", cfg->FIELD_NAME); \
    return cfg->FIELD_NAME;\
  }

POSTPROC_FIELD(PostProcFlag, post_proc_flag, int)
POSTPROC_FIELD(DeblockingLevel, deblocking_level, int)
POSTPROC_FIELD(NoiseLevel, noise_level, int)

#define DEC_CFG_FIELD(JNI_NAME, FIELD_NAME, TYPE) \
  FUNC(void, vpxCodecDecSet ##JNI_NAME, jlong jcfg, jint jparam) { \
    printf("vpxCodecDecSet" #JNI_NAME); \
    printf("Setting cfg->" #FIELD_NAME " = %d", jparam); \
    vpx_codec_dec_cfg_t *cfg = reinterpret_cast<vpx_codec_dec_cfg_t*>(jcfg); \
    cfg->FIELD_NAME = (TYPE)jparam;\
  } \
  FUNC(int, vpxCodecDecGet ##JNI_NAME, jlong jcfg) { \
    printf("vpxCodecDecGet" #JNI_NAME); \
    const vpx_codec_dec_cfg_t *cfg = \
        reinterpret_cast<vpx_codec_dec_cfg_t*>(jcfg); \
    printf("Getting cfg->" #FIELD_NAME " = %d", cfg->FIELD_NAME); \
    return cfg->FIELD_NAME;\
  }

DEC_CFG_FIELD(Threads, threads, unsigned int)
DEC_CFG_FIELD(Width, w, unsigned int)
DEC_CFG_FIELD(Height, h, unsigned int)

FUNC(jint, vpxCodecDecDecode, jlong jctx, jbyteArray jbuf, jint buf_offset, jint buf_sz) {
  printf("vpxCodecDecDecode");
  vpx_codec_ctx_t *ctx = reinterpret_cast<vpx_codec_ctx_t *>(jctx);
  jbyte *buf = env->GetByteArrayElements(jbuf, 0);
  // egergo
  buf += buf_offset;

  int ret = vpx_codec_decode(
      ctx, reinterpret_cast<const uint8_t*>(buf), buf_sz, NULL, 0);

  env->ReleaseByteArrayElements(jbuf, buf, 0);

  return ret;
}

FUNC(jbyteArray, vpxCodecDecGetFrame, jlong jctx) {
  printf("vpxCodecDecGetFrame");
  vpx_codec_ctx_t *ctx = reinterpret_cast<vpx_codec_ctx_t *>(jctx);
  vpx_codec_iter_t iter = NULL;
  vpx_image_t *img = vpx_codec_get_frame(ctx, &iter);

  if (img == NULL)
    return NULL;

  unsigned int start = 0;
  unsigned int frameSize = (img->d_w * img->d_h)
                            + 2 * (((1 + img->d_w) / 2)
                                * ((1 + img->d_h) / 2));

  jbyteArray jb = env->NewByteArray(frameSize);

  uint8_t *buf = img->planes[VPX_PLANE_Y];
  unsigned int length = img->d_w;
  for (int y = 0; y < img->d_h; ++y) {
    env->SetByteArrayRegion(jb, start, length, reinterpret_cast<jbyte*>(buf));
    buf += img->stride[VPX_PLANE_Y];
    start += length;
  }

  buf = img->planes[VPX_PLANE_U];
  length = (1 + img->d_w) / 2;
  for (int y = 0; y < (1 + img->d_h) / 2; ++y) {
    env->SetByteArrayRegion(jb, start, length, reinterpret_cast<jbyte*>(buf));
    buf += img->stride[VPX_PLANE_U];
    start += length;
  }

  buf = img->planes[VPX_PLANE_V];
  for (int y = 0; y < (1 + img->d_h) / 2; ++y) {
    env->SetByteArrayRegion(jb, start, length, reinterpret_cast<jbyte*>(buf));
    buf += img->stride[VPX_PLANE_V];
    start += length;
  }

  return jb;
}
