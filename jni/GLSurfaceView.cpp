#include <string.h>
#include <jni.h>
#include <android/log.h>

#include <android/log.h>

extern "C" {
JNIEXPORT jint
JNICALL
Java_com_showkith264_MainActivity_stringFromJNICPP(JNIEnv *env, jclass cls,
		jbyteArray array, jint array_length);

}
;

JNIEXPORT jint JNICALL
Java_com_showkith264_MainActivity_stringFromJNICPP(JNIEnv *env, jclass cls, jbyteArray array, jint array_length)
{

	jintArray messagae;
	jbyte* content_array = (env)->GetByteArrayElements(array,NULL);
	int length=array_length;
	for (int i=0; i < 500; i++)
	{
		__android_log_print(ANDROID_LOG_VERBOSE, "testing...", "Value %d", content_array[i]);
	}
	return array_length;

}

