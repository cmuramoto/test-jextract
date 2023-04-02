package test;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class JMHRunner {

	public static void main(String[] args) throws RunnerException {
		var opts = new OptionsBuilder() //
				.include("Reader*|Estimate*") //
				.forks(1) //
				.warmupForks(1) //
				.warmupIterations(1) //
				.measurementIterations(3) //
				.jvmArgs("--enable-preview", //
						"--enable-native-access=ALL-UNNAMED", //
						"--add-modules", "jdk.internal.vm.ci", //
						"--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED", //
						"--add-exports", "jdk.internal.vm.ci/jdk.vm.ci.code=ALL-UNNAMED",
						"--add-exports", "jdk.internal.vm.ci/jdk.vm.ci.code.site=ALL-UNNAMED",
						"--add-exports", "jdk.internal.vm.ci/jdk.vm.ci.hotspot=ALL-UNNAMED",
						"--add-exports", "jdk.internal.vm.ci/jdk.vm.ci.meta=ALL-UNNAMED",
						"--add-exports", "jdk.internal.vm.ci/jdk.vm.ci.runtime=ALL-UNNAMED",
						"-XX:+UnlockExperimentalVMOptions", "-XX:+EnableJVMCI",
						"-XX:+UseSerialGC", "-XX:+AlwaysPreTouch",
						"-Xms256m", "-Xmx256m", "-XX:MaxDirectMemorySize=4G", //
						"-Djava.library.path=native")
				.build(); //

		new Runner(opts).run();
	}

}
