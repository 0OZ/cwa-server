package app.coronawarn.server.services.distribution.runner;

import app.coronawarn.server.common.persistence.service.DiagnosisKeyService;
import app.coronawarn.server.services.distribution.Application;
import app.coronawarn.server.services.distribution.assembly.component.CwaApiStructureProvider;
import app.coronawarn.server.services.distribution.assembly.component.OutputDirectoryProvider;
import app.coronawarn.server.services.distribution.persistence.domain.ExportConfiguration;
import app.coronawarn.server.services.distribution.persistence.service.ExportBatchService;
import app.coronawarn.server.services.distribution.persistence.service.ExportConfigurationService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * This runner reads the configurations and spawns one {@link Assembly} runner for each active configuration.
 */
@Component
@Order(2)
public class ExportRunner implements ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(Assembly.class);

  private final OutputDirectoryProvider outputDirectoryProvider;

  private final CwaApiStructureProvider cwaApiStructureProvider;

  private final ExportConfigurationService exportConfigurationService;

  private final ExportBatchService exportBatchService;

  private final DiagnosisKeyService diagnosisKeyService;

  private final ApplicationContext applicationContext;

  /**
   * Creates an ExportRunner Runner, using {@link OutputDirectoryProvider}, {@link CwaApiStructureProvider}
   * and {@link ApplicationContext}.
   */
  @Autowired
  public ExportRunner(OutputDirectoryProvider outputDirectoryProvider, CwaApiStructureProvider cwaApiStructureProvider,
                ExportConfigurationService exportConfigurationService, ExportBatchService exportBatchService,
                      DiagnosisKeyService diagnosisKeyService, ApplicationContext applicationContext) {
    this.outputDirectoryProvider = outputDirectoryProvider;
    this.cwaApiStructureProvider = cwaApiStructureProvider;
    this.exportConfigurationService = exportConfigurationService;
    this.exportBatchService = exportBatchService;
    this.diagnosisKeyService = diagnosisKeyService;
    this.applicationContext = applicationContext;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      List<ExportConfiguration> configurations = this.exportConfigurationService.getExportConfigurations();
      logger.debug("Loaded " + configurations.size() + " export configurations.");

      for (ExportConfiguration configuration: configurations) {
        if (configuration.isActive()) {
          Assembly assemblyRunner = new Assembly(this.outputDirectoryProvider, this.cwaApiStructureProvider,
                  this.exportBatchService, configuration, this.diagnosisKeyService, this.applicationContext);
          assemblyRunner.run();
        }
      }
    } catch (Exception e) {
      logger.error("ExportRunner Runner failed.", e);
      Application.killApplication(applicationContext);
    }
  }
}
