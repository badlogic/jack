package com.badlogic.jack;

import com.badlogic.jack.build.AntScriptGenerator;
import com.badlogic.jack.build.BuildConfig;
import com.badlogic.jack.build.BuildExecutor;
import com.badlogic.jack.build.BuildTarget;
import com.badlogic.jack.build.BuildTarget.TargetOs;

public class JackBuild {
	public static void main(String[] args) {
		BuildConfig config = new BuildConfig("jack", "target", "libs", "native");
		BuildTarget win32home = BuildTarget.newDefaultTarget(TargetOs.Windows, false);
		win32home.compilerPrefix = "";
		new AntScriptGenerator().generate(config, win32home);
		BuildExecutor.executeAnt("native/build-windows32.xml", "-v");
	}
}
