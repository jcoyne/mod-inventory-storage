package org.folio.services;

import static org.folio.services.SystemCallNumberType.DEWEY_DECIMAL;
import static org.folio.services.SystemCallNumberType.LIBRARY_OF_CONGRESS;
import static org.folio.services.SystemCallNumberType.NATIONAL_LIBRARY_OF_MEDICINE;
import static org.folio.services.SystemCallNumberType.SUPERINTENDENT_OF_DOCUMENTS;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.marc4j.callnum.CallNumber;
import org.marc4j.callnum.DeweyCallNumber;
import org.marc4j.callnum.LCCallNumber;
import org.marc4j.callnum.NlmCallNumber;

public final class CallNumberUtils {

  private static final Map<String, Function<String, Optional<String>>> logCallNumberMap = new HashMap<>();

  private CallNumberUtils() {
  }

  static {
    logCallNumberMap.put(DEWEY_DECIMAL.getId(), (String cn) -> getValidShelfKey(new DeweyCallNumber(cn)));
    logCallNumberMap.put(LIBRARY_OF_CONGRESS.getId(), (String cn) -> getValidShelfKey(new LCCallNumber(cn)));
    logCallNumberMap.put(NATIONAL_LIBRARY_OF_MEDICINE.getId(), (String cn) -> getValidShelfKey(new NlmCallNumber(cn)));
    logCallNumberMap.put(SUPERINTENDENT_OF_DOCUMENTS.getId(), (String cn) -> getValidShelfKey(new SuDocCallNumber(cn)));
  }

  public static Optional<String> getShelfKeyFromCallNumber(String callNumberTypeId, String callNumber) {
    var function = logCallNumberMap.get(callNumberTypeId);
    if (function != null) {
      return function.apply(callNumber)
        .map(String::trim);
    }

    return Optional.ofNullable(callNumber)
      .map(String::trim);
  }

  private static Optional<String> getValidShelfKey(CallNumber value) {
    return Optional.of(value)
      .filter(CallNumber::isValid)
      .map(CallNumber::getShelfKey);
  }
}
