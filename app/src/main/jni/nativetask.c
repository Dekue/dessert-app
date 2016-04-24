/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "DESSERT -> libNativeTasks"

#include "jni.h"
#include <android/log.h>

#define LOGI(...) do { __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__); } while(0)
//#define LOGW(...) do { __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__); } while(0)
#define LOGE(...) do { __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__); } while(0)

#include <sys/types.h>
#include <sys/wait.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
//#include <string.h>
//#include <sys/ioctl.h>
//#include <pthread.h>
//#include <errno.h>
//#include <fcntl.h>
//#include <termios.h>

//static jclass class_fileDescriptor;
//static jfieldID field_fileDescriptor_descriptor;
//static jmethodID method_fileDescriptor_init;

/*---------------------------------------------------------------------
 * internal methods
 */
static int create_subprocess(const char *cmd, const char *filename, const char *arg0, const char *arg1, const char *arg2)
{
	pid_t pid = fork();
	if (pid < 0) {
		LOGE("- fork failed: %s -\n", strerror(errno));
		return -1;
	}

	if (pid == 0) {
		// child process
		setsid();

		execl(cmd, filename, arg0, arg1, arg2, NULL);
		exit(-1);
	} else {
		// parent process
		LOGI("new process with pid %d", pid);
	}

	return pid;
}

/*---------------------------------------------------------------------
 * JNI calls
 */

static jstring Java_de_fuberlin_dessert_tasks_NativeTasks_getEnvironmentValue(JNIEnv *env, jclass class, jstring key) {
	const char *keyString = (*env)->GetStringUTFChars(env, key, 0);

	LOGI("getEnvironmentValue: %s", keyString);

	char * pPath = getenv(keyString);
	(*env)->ReleaseStringUTFChars(env, key, keyString);
	return (*env)->NewStringUTF(env, pPath);
}

static jint Java_de_fuberlin_dessert_tasks_NativeTasks_runCommand(JNIEnv *env, jclass class, jstring command) {
	const char *commandString = (*env)->GetStringUTFChars(env, command, 0);

	LOGI("runCommand: %s", commandString);

	int exitcode = system(commandString);

	LOGI("exit code %d", exitcode);

	(*env)->ReleaseStringUTFChars(env, command, commandString);
	return (jint) exitcode;
}

static jint Java_de_fuberlin_dessert_tasks_NativeTasks_waitFor(JNIEnv *env, jclass class, jint pid) {
    int result = -1;

    LOGI("waitFor: %d", pid);

    int status = 0;
	waitpid(pid, &status, 0);
	LOGI("status: %d", status);
	if (WIFEXITED(status)) {
		result = WEXITSTATUS(status);
	}

	LOGI("exit code %d", result);

	return (jint) result;
}

static jint Java_de_fuberlin_dessert_tasks_NativeTasks_createSubprocess(JNIEnv *env, jclass class, jstring command, jstring arg0, jstring arg1, jstring arg2) {
	const char *commandString = (*env)->GetStringUTFChars(env, command, 0);
	const char *filenameString = commandString;
	const char *arg0String = (*env)->GetStringUTFChars(env, arg0, 0);
	const char *arg1String = (*env)->GetStringUTFChars(env, arg1, 0);
	const char *arg2String = (*env)->GetStringUTFChars(env, arg2, 0);

	// find last '/' in commandString
	size_t idx;
	for (idx = strlen(commandString); idx > 0; --idx) {
		if (commandString[idx] == '/') {
			filenameString = commandString + idx + 1;
			break;
		}
	}

	LOGI("createSubprocess: %s %s %s %s %s", commandString, filenameString, (arg0String != NULL ? arg0String : "<null>"), (arg1String != NULL ? arg1String : "<null>"), (arg2String != NULL ? arg2String : "<null>"));

	int pid = create_subprocess(commandString, filenameString, arg0String, arg1String, arg2String);

	(*env)->ReleaseStringUTFChars(env, command, commandString);
	(*env)->ReleaseStringUTFChars(env, arg0, arg0String);
	(*env)->ReleaseStringUTFChars(env, arg1, arg1String);
	(*env)->ReleaseStringUTFChars(env, arg2, arg2String);
	return (jint) pid;
}



/*------------------------------------------------------------------------
 * JNI Registration
 */
static const char *classPathName = "de/fuberlin/dessert/tasks/NativeTasks";
static JNINativeMethod method_table[] =
	{
		{
			"getEnvironmentValue",
			"(Ljava/lang/String;)Ljava/lang/String;",
			(void*) Java_de_fuberlin_dessert_tasks_NativeTasks_getEnvironmentValue
		},
		{
			"runCommand",
			"(Ljava/lang/String;)I",
			(void*) Java_de_fuberlin_dessert_tasks_NativeTasks_runCommand
		},
		{
			"createSubprocess",
			"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I",
			(void*) Java_de_fuberlin_dessert_tasks_NativeTasks_createSubprocess
		},
		{
			"waitFor",
			"(I)I",
			(void*) Java_de_fuberlin_dessert_tasks_NativeTasks_waitFor
		}
	};

static int registerNativeMethods(JNIEnv* env, const char* className, JNINativeMethod* gMethods, int numMethods) {
	jclass clazz;

	LOGI("registerNativeMethods");

	clazz = (*env)->FindClass(env, className);
	if (clazz == NULL) {
		LOGE("Native registration unable to find class '%s'", className);
		return JNI_FALSE;
	}
	if ((*env)->RegisterNatives(env, clazz, gMethods, numMethods) < 0) {
		LOGE("RegisterNatives failed for '%s'", className);
		return JNI_FALSE;
	}

	return JNI_TRUE;
}

static int registerNatives(JNIEnv* env) {
	if (!registerNativeMethods(env, classPathName, method_table, sizeof(method_table) / sizeof(method_table[0]))) {
		return JNI_FALSE;
	}

	return JNI_TRUE;
}

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
	JNIEnv* env = NULL;

	LOGI("JNI_OnLoad");

	if ((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_4) != JNI_OK) {
		LOGE("ERROR: GetEnv failed");
		return -1;
	}

	if (registerNatives(env) != JNI_TRUE) {
		LOGE("ERROR: registerNatives failed");
		return -1;
	}

	return JNI_VERSION_1_4;
}
