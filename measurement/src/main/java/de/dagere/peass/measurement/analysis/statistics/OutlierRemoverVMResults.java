package de.dagere.peass.measurement.analysis.statistics;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.StatisticsConfig;
import de.dagere.peass.measurement.rca.data.OneVMResult;

public class OutlierRemoverVMResults {

   private static final Logger LOG = LogManager.getLogger(OutlierRemoverVMResults.class);

   public static void getValuesWithoutOutliers(final List<OneVMResult> results, final SummaryStatistics statistics, final StatisticsConfig config) {
      if (config.getOutlierFactor() != 0 && results.size() > 1) {
         final SummaryStatistics fullStatistic = new SummaryStatistics();
         addAll(results, fullStatistic);

         double min = fullStatistic.getMean() - config.getOutlierFactor() * fullStatistic.getStandardDeviation();
         double max = fullStatistic.getMean() + config.getOutlierFactor() * fullStatistic.getStandardDeviation();

         LOG.debug("Removing outliers that are not between {} and {} - Old vm count: {}", min, max, results.size());
         addNonOutlier(results, statistics, min, max);
         LOG.debug("Final VM count: {}", statistics.getN());
      } else {
         addAll(results, statistics);
      }

   }

   private static void addNonOutlier(final List<OneVMResult> results, final SummaryStatistics statistics, final double min, final double max) {
      for (final OneVMResult result : results) {
         final double average = result.getAverage();
         if (average >= min && average <= max) {
            statistics.addValue(average);
            LOG.trace("Adding value: {}", average);
         } else {
            LOG.debug("Not adding outlier: {}", average);
         }
      }
   }

   private static void addAll(final List<OneVMResult> results, final SummaryStatistics statistics) {
      for (final OneVMResult result : results) {
         final double average = result.getAverage();
         statistics.addValue(average);
      }
   }
}
