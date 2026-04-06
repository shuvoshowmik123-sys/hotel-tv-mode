@echo off
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
echo Accepting SDK licenses...
echo y | "C:\Users\U I S\AppData\Local\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
echo Starting Gradle build...
"C:\Users\U I S\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9\bin\gradle.bat" assembleDebug --no-daemon --info > build_output.txt 2>&1
echo RESULT=%ERRORLEVEL% >> build_output.txt
echo BUILD_DONE >> build_output.txt
