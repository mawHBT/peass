package de.dagere.peass.config;

import net.kieker.sourceinstrumentation.AllowedKiekerRecord;

public class KiekerConfiguration {
   private boolean useKieker = false;
   private boolean useSourceInstrumentation = true;
   private boolean useSelectiveInstrumentation = true;
   private boolean useSampling = false;
   private boolean useCircularQueue = true;
   private boolean enableAdaptiveMonitoring = false;
   private int kiekerAggregationInterval = 5000;
   private AllowedKiekerRecord record = AllowedKiekerRecord.OPERATIONEXECUTION;

   public KiekerConfiguration() {
   }

   public KiekerConfiguration(final boolean useKieker) {
      this.useKieker = useKieker;
   }

   public KiekerConfiguration(final KiekerConfiguration other) {
      this.useKieker = other.useKieker;
      this.useSourceInstrumentation = other.useSourceInstrumentation;
      this.useSelectiveInstrumentation = other.useSelectiveInstrumentation;
      this.useSampling = other.useSampling;
      this.useCircularQueue = other.useCircularQueue;
      this.enableAdaptiveMonitoring = other.enableAdaptiveMonitoring;
      this.kiekerAggregationInterval = other.kiekerAggregationInterval;
      this.record = other.record;
   }

   public boolean isUseKieker() {
      return useKieker;
   }

   public void setUseKieker(final boolean useKieker) {
      this.useKieker = useKieker;
   }

   public boolean isUseSourceInstrumentation() {
      return useSourceInstrumentation;
   }

   public void setUseSourceInstrumentation(final boolean useSourceInstrumentation) {
      this.useSourceInstrumentation = useSourceInstrumentation;
   }

   public boolean isUseSelectiveInstrumentation() {
      return useSelectiveInstrumentation;
   }

   public void setUseSelectiveInstrumentation(final boolean useSelectiveInstrumentation) {
      this.useSelectiveInstrumentation = useSelectiveInstrumentation;
   }

   public boolean isUseSampling() {
      return useSampling;
   }

   public void setUseSampling(final boolean useSampling) {
      this.useSampling = useSampling;
   }

   public boolean isUseCircularQueue() {
      return useCircularQueue;
   }

   public void setUseCircularQueue(final boolean useCircularQueue) {
      this.useCircularQueue = useCircularQueue;
   }

   public boolean isEnableAdaptiveMonitoring() {
      return enableAdaptiveMonitoring;
   }

   public void setEnableAdaptiveMonitoring(final boolean enableAdaptiveMonitoring) {
      this.enableAdaptiveMonitoring = enableAdaptiveMonitoring;
   }

   public int getKiekerAggregationInterval() {
      return kiekerAggregationInterval;
   }

   public void setKiekerAggregationInterval(final int kiekerAggregationInterval) {
      this.kiekerAggregationInterval = kiekerAggregationInterval;
   }

   public AllowedKiekerRecord getRecord() {
      return record;
   }

   public void setRecord(final AllowedKiekerRecord record) {
      if (record == null) {
         this.record = AllowedKiekerRecord.OPERATIONEXECUTION;
      } else {
         this.record = record;
      }
   }
}