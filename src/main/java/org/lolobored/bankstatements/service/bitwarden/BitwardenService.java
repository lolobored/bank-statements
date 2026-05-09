package org.lolobored.bankstatements.service.bitwarden;

import org.lolobored.bankstatements.model.config.Bank;

import java.io.IOException;

public interface BitwardenService {
  void checkVaultAccess() throws IOException, InterruptedException;
  void resolveCredentials(Bank bank) throws IOException, InterruptedException;
}
