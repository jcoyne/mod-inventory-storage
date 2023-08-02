package org.folio.services;

import lombok.Getter;

@Getter
public enum SystemCallNumberType {
  DEWEY_DECIMAL("03dd64d0-5626-4ecd-8ece-4531e0069f35", "Dewey Decimal classification"),
  LIBRARY_OF_CONGRESS("95467209-6d7b-468b-94df-0f5d7ad2747d", "Library of Congress classification"),
  NATIONAL_LIBRARY_OF_MEDICINE("054d460d-d6b9-4469-9e37-7a78a2266655", "National Library of Medicine classification"),
  SUPERINTENDENT_OF_DOCUMENTS("fc388041-6cd0-4806-8a74-ebe3b9ab4c6e", "Superintendent of Documents classification"),
  OTHER_SCHEME("6caca63e-5651-4db6-9247-3205156e9699", "Other scheme");

  private final String id;
  private final String name;

  SystemCallNumberType(String id, String name) {
    this.id = id;
    this.name = name;
  }
}
