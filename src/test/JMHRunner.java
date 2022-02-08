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
				.warmupIterations(3) //
				.measurementIterations(3) //
				.jvmArgs("--enable-preview", //
						"--enable-native-access=ALL-UNNAMED", //
						"--add-modules", "jdk.incubator.foreign", //
						"--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED", //
						"-Xms256m", "-Xmx256m", "-XX:MaxDirectMemorySize=4G", //
						"-Djava.library.path=native")
				.build(); //

		new Runner(opts).run();
	}

}
