// Copyright 2012 Google Inc. All Rights Reserved.
// Author: frkoenig@google.com (Fritz Koenig)
#include <assert.h>
#include <jni.h>
#include <string.h>
#include <new>

#include "vpx/vpx_encoder.h"
#include "vpx/vp8cx.h"
#include "vpx_ports/mem_ops.h"

#ifdef NDEBUG
# define printf(fmt, ...)
#else
# ifdef __ANDROID__
#  include <android/log.h>
#  define printf(fmt, ...) \
   __android_log_print(ANDROID_LOG_DEBUG, "LIBVPX_ENC_CFG", fmt, ##__VA_ARGS__)
# else
#  define printf(fmt, ...) \
   printf(fmt "\n", ##__VA_ARGS__)
# endif
#endif

#define FUNC(RETURN_TYPE, NAME, ...) \
  extern "C" { \
  JNIEXPORT RETURN_TYPE Java_com_google_libvpx_LibVpxEncConfig_ ## NAME \
                      (JNIEnv * env, jobject thiz, ##__VA_ARGS__);\
  } \
  JNIEXPORT RETURN_TYPE Java_com_google_libvpx_LibVpxEncConfig_ ## NAME \
                      (JNIEnv * env, jobject thiz, ##__VA_ARGS__)\

#define CONFIG_FIELD(JNI_NAME, FIELD_NAME, TYPE) \
  FUNC(void, vpxCodecEncSet ##JNI_NAME, jlong jcfg, jint jparam) { \
    printf("vpxCodecEncSet" #JNI_NAME); \
    printf("Setting cfg->" #FIELD_NAME " = %d", jparam); \
    vpx_codec_enc_cfg_t *cfg = reinterpret_cast<vpx_codec_enc_cfg_t*>(jcfg); \
    cfg->FIELD_NAME = (TYPE)jparam;\
  } \
  FUNC(int, vpxCodecEncGet ##JNI_NAME, jlong jcfg) { \
    printf("vpxCodecEncGet" #JNI_NAME); \
    const vpx_codec_enc_cfg_t *cfg = \
        reinterpret_cast<vpx_codec_enc_cfg_t*>(jcfg); \
    printf("Getting cfg->" #FIELD_NAME " = %d", cfg->FIELD_NAME); \
    return cfg->FIELD_NAME;\
  }

static const struct codec_item {
  const char              *name;
  const vpx_codec_iface_t *iface;
  unsigned int             fourcc;
} codecs[] = {
  {"vp8",  &vpx_codec_vp8_cx_algo, 0x30385056},
};

CONFIG_FIELD(Usage, g_usage, unsigned int)
CONFIG_FIELD(Threads, g_threads, unsigned int)
CONFIG_FIELD(Profile, g_profile, unsigned int)
CONFIG_FIELD(Width, g_w, unsigned int)
CONFIG_FIELD(Height, g_h, unsigned int)
FUNC(void, vpxCodecEncSetTimebase, jlong jcfg, jint jnum, jint jden) {
  printf("vpxCodecEncSetTimebase");
  printf("Setting cfg->g_timebase.num = %d", jnum);
  printf("Setting cfg->g_timebase.den = %d", jden);
  vpx_codec_enc_cfg_t *cfg = reinterpret_cast<vpx_codec_enc_cfg_t*>(jcfg);
  cfg->g_timebase.num = jnum;
  cfg->g_timebase.den = jden;
}

FUNC(jobject, vpxCodecEncGetTimebase, jlong jcfg) {
  printf("vpxCodecEncGetTimebase");
  vpx_codec_enc_cfg_t *cfg = reinterpret_cast<vpx_codec_enc_cfg_t*>(jcfg);

  printf("Getting cfg->g_timebase.num = %d", cfg->g_timebase.num);
  printf("Getting cfg->g_timebase.den = %d", cfg->g_timebase.den);

  jclass rational = env->FindClass("com/google/libvpx/Rational");
  assert(rational != NULL);

  jmethodID rationalInitMethodId =
      env->GetMethodID(rational, "<init>", "(JJ)V");
  assert(rationalInitMethodId != NULL);

  jobject rationalNumber = env->NewObject(rational,
                                          rationalInitMethodId,
                                          (jlong)cfg->g_timebase.num,
                                          (jlong)cfg->g_timebase.den);

  return rationalNumber;
}
CONFIG_FIELD(ErrorResilient, g_error_resilient, vpx_codec_er_flags_t)
CONFIG_FIELD(Pass, g_pass, vpx_enc_pass)
CONFIG_FIELD(LagInFrames, g_lag_in_frames, unsigned int)
CONFIG_FIELD(RCDropframeThresh, rc_dropframe_thresh, unsigned int)
CONFIG_FIELD(RCResizeAllowed, rc_resize_allowed, unsigned int)
CONFIG_FIELD(RCResizeUpThresh, rc_resize_up_thresh, unsigned int)
CONFIG_FIELD(RCResizeDownThresh, rc_resize_down_thresh, unsigned int)
CONFIG_FIELD(RCEndUsage, rc_end_usage, vpx_rc_mode)
FUNC(void, vpxCodecEncSetRCTwoPassStatsIn, jlong jcfg, jlong jbuf) {
  vpx_codec_enc_cfg_t *cfg = reinterpret_cast<vpx_codec_enc_cfg_t*>(jcfg);
  struct vpx_fixed_buf *buf = (struct vpx_fixed_buf *)jbuf;

  memcpy(&cfg->rc_twopass_stats_in, buf, sizeof(struct vpx_fixed_buf));
}
CONFIG_FIELD(RCTargetBitrate, rc_target_bitrate, unsigned int)
CONFIG_FIELD(RCMinQuantizer, rc_min_quantizer, unsigned int)
CONFIG_FIELD(RCMaxQuantizer, rc_max_quantizer, unsigned int)
CONFIG_FIELD(RCUndershootPct, rc_undershoot_pct, unsigned int)
CONFIG_FIELD(RCOvershootPct, rc_overshoot_pct, unsigned int)
CONFIG_FIELD(RCBufSz, rc_buf_sz, unsigned int)
CONFIG_FIELD(RCBufInitialSz, rc_buf_initial_sz, unsigned int)
CONFIG_FIELD(RCBufOptimalSz, rc_buf_optimal_sz, unsigned int)
CONFIG_FIELD(RC2PassVBRBiasPct, rc_2pass_vbr_bias_pct, unsigned int)
CONFIG_FIELD(RC2PassVBRMinsectionPct, rc_2pass_vbr_minsection_pct,
                                      unsigned int)
CONFIG_FIELD(RC2PassVBRMaxsectioniasPct, rc_2pass_vbr_maxsection_pct,
                                         unsigned int)
CONFIG_FIELD(KFMode, kf_mode, vpx_kf_mode)
CONFIG_FIELD(KFMinDist, kf_min_dist, unsigned int)
CONFIG_FIELD(KFMaxDist, kf_max_dist, unsigned int)

FUNC(jlong, vpxCodecEncAllocCfg) {
  printf("vpxCodecEncAllocCfg");

  const vpx_codec_enc_cfg_t *cfg = new (std::nothrow) vpx_codec_enc_cfg_t;
  return (intptr_t)cfg;
}

FUNC(void, vpxCodecEncFreeCfg, jlong jcfg) {
  printf("vpxCodecEncFreeCfg");

  const vpx_codec_enc_cfg_t *cfg = reinterpret_cast<vpx_codec_enc_cfg_t*>(jcfg);
  delete cfg;
}

FUNC(jlong, vpxCodecEncConfigDefault, jlong jcfg, jint arg_usage) {
  printf("vpxCodecEncConfigDefault");

  vpx_codec_enc_cfg_t *cfg = reinterpret_cast<vpx_codec_enc_cfg_t*>(jcfg);
  const struct codec_item *codec = codecs;
  return vpx_codec_enc_config_default(codec->iface, cfg, arg_usage);
}

FUNC(jint, vpxCodecEncGetFourcc) {
  printf("vpxCodecEncGetFourcc");
  const struct codec_item  *codec = codecs;
  return codec->fourcc;
}
