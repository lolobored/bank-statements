package org.lolobored.bankstatements.service.bitwarden;

import java.io.IOException;
import org.lolobored.bankstatements.model.config.Bank;

public interface BitwardenService {
  void checkVaultAccess() throws IOException, InterruptedException;

  void resolveCredentials(Bank bank) throws IOException, InterruptedException;
}
