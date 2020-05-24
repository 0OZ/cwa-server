/*
 * Corona-Warn-App
 *
 * SAP SE and all other contributors /
 * copyright owners license this file to you under the Apache
 * License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package app.coronawarn.server.services.distribution.runner;

import app.coronawarn.server.common.persistence.service.DiagnosisKeyService;
import app.coronawarn.server.services.distribution.Application;
import app.coronawarn.server.services.distribution.assembly.component.CwaApiStructureProvider;
import app.coronawarn.server.services.distribution.assembly.component.OutputDirectoryProvider;
import app.coronawarn.server.services.distribution.assembly.structure.WritableOnDisk;
import app.coronawarn.server.services.distribution.assembly.structure.directory.Directory;
import app.coronawarn.server.services.distribution.assembly.structure.util.ImmutableStack;
import app.coronawarn.server.services.distribution.persistence.domain.ExportBatch;
import app.coronawarn.server.services.distribution.persistence.domain.ExportBatchStatus;
import app.coronawarn.server.services.distribution.persistence.domain.ExportConfiguration;
import app.coronawarn.server.services.distribution.persistence.service.ExportBatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * This runner assembles and writes diagnosis key bundles and the parameter configuration.
 */
public class Assembly implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(Assembly.class);

  private final OutputDirectoryProvider outputDirectoryProvider;

  private final CwaApiStructureProvider cwaApiStructureProvider;

  private final ExportBatchService exportBatchService;

  private final ExportConfiguration exportConfiguration;

  private final DiagnosisKeyService diagnosisKeyService;

  private final ApplicationContext applicationContext;

  /**
   * Creates an Assembly, using {@link OutputDirectoryProvider}, {@link CwaApiStructureProvider} and
   * {@link ApplicationContext}.
   */
  public Assembly(OutputDirectoryProvider outputDirectoryProvider, CwaApiStructureProvider cwaApiStructureProvider,
                  ExportBatchService exportBatchService, ExportConfiguration exportConfiguration,
                  DiagnosisKeyService diagnosisKeyService, ApplicationContext applicationContext) {
    this.outputDirectoryProvider = outputDirectoryProvider;
    this.cwaApiStructureProvider = cwaApiStructureProvider;
    this.exportBatchService = exportBatchService;
    this.exportConfiguration = exportConfiguration;
    this.diagnosisKeyService = diagnosisKeyService;
    this.applicationContext = applicationContext;
  }

  // TODO: maybe error handling, if no diagnosis keys are found, if that is possible?
  private Instant getExportStartDateTime() {
    return this.exportBatchService.getLatestBatch()
            .map(ExportBatch::getFromTimestamp)
            .orElse(Instant.ofEpochSecond(this.diagnosisKeyService.getOldestDiagnosisKeyAfterTimestamp(
                    this.exportConfiguration.getFromTimestamp().getEpochSecond() / 3600).getSubmissionTimestamp()
                    * 3600));
  }

  /**
   * Starts the Assembly runner.
   */
  @Override
  public void run() {
    try {
      Directory<WritableOnDisk> outputDirectory = this.outputDirectoryProvider.getDirectory();
      // TODO

      // Read last export batch from database
      // use to timestamp as new from timestamp and create batches accordingly
      // save batches to db
      // start processing individual batches
      // update batches once finished

      // if no batches exist select oldest diagnosis key from database, that is younger than config fromTimestamp
      // generate timestamp based on that

      Instant exportStart  = getExportStartDateTime();

      while (exportStart.isBefore(Instant.now())) {
        outputDirectory.addWritable(cwaApiStructureProvider.getDirectory(ExportBatch.builder()
                .withFromTimestamp(exportStart)
                .withToTimestamp(exportStart.plus(this.exportConfiguration.getPeriod(), ChronoUnit.HOURS))
                .withStatus(ExportBatchStatus.OPEN)
                .withExportConfiguration(this.exportConfiguration)
                .build()));
//        this.outputDirectoryProvider.clear();
//        logger.debug("Preparing files...");
//        outputDirectory.prepare(new ImmutableStack<>());
//        logger.debug("Writing files...");
//        outputDirectory.write();
        exportStart = exportStart.plus(this.exportConfiguration.getPeriod(), ChronoUnit.HOURS);
        logger.info(exportStart.toString());
      }
    } catch (Exception e) {
      logger.error("Distribution data assembly failed.", e);
      Application.killApplication(applicationContext);
    }

    logger.debug("Distribution data assembled successfully.");
  }
}
