package de.dagere.peass;

import java.io.File;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Test;

import de.dagere.peass.dependency.ClazzFileFinder;

public class TestPackageFinder {
	
	@Test
	public void testDependencyModule(){
		final List<String> lowestPackage = ClazzFileFinder.getClasses(new File("."));
		System.out.println(lowestPackage);
		MatcherAssert.assertThat(lowestPackage, IsIterableContaining.hasItem("de.dagere.peass.DependencyReadingParallelStarter"));
		MatcherAssert.assertThat(lowestPackage, Matchers.not(IsIterableContaining.hasItem("de.dagere.peass.DependencyReadingParallelStarter.DependencyReadingParallelStarter")));
		MatcherAssert.assertThat(lowestPackage, IsIterableContaining.hasItem("de.dagere.peass.statistics.DependencyStatisticAnalyzer"));
		MatcherAssert.assertThat(lowestPackage, IsIterableContaining.hasItem("de.dagere.peass.statistics.DependencyStatistics"));
		MatcherAssert.assertThat(lowestPackage, IsIterableContaining.hasItem("de.dagere.peass.TestPackageFinder"));
	}
}
