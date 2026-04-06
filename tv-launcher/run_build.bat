@echo off
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
"C:\Users\U I S\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9\bin\gradle.bat" assembleDebug --no-daemon --console=plain > build_result.txt 2>&1
echo DONE >> build_result.txt
