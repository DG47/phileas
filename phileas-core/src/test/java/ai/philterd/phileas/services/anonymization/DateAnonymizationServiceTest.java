/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.services.anonymization;

import ai.philterd.phileas.services.context.DefaultContextService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.regex.Pattern;

public class DateAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(DateAnonymizationServiceTest.class);

    // The MMMM dd, yyyy format a month-name date is replaced with.
    private static final Pattern MONTH_NAME_FORMAT = Pattern.compile("[A-Z][a-z]+ \\d{2}, \\d{4}");

    @Test
    public void anonymize1() {

        AnonymizationService anonymizationService = new DateAnonymizationService();

        final String token = "11-18-2018";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Date: " + replacement);
        Assertions.assertNotEquals(token, replacement);

    }

    @Test
    public void anonymize2() {

        AnonymizationService anonymizationService = new DateAnonymizationService();

        final String token = "April 1, 2019";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Date: " + replacement);
        Assertions.assertNotEquals(token, replacement);

    }

    @Test
    public void anonymizeMonthAbbreviationWithPeriodKeepsMonthNameFormat() {

        final AnonymizationService anonymizationService = new DateAnonymizationService();

        for (final String token : new String[]{"Sep. 5, 2020", "Sept. 5, 2020", "Jan. 1, 2019", "Dec. 25, 2018", "sep. 5, 2020", "Sep. 5th, 2020"}) {

            final String replacement = anonymizationService.anonymize(token);

            LOGGER.info("Date: " + token + " -> " + replacement);
            Assertions.assertNotEquals(token, replacement);
            Assertions.assertTrue(MONTH_NAME_FORMAT.matcher(replacement).matches(), token + " was replaced with " + replacement);

        }

    }

    @Test
    public void anonymizeMonthAbbreviationWithoutPeriodKeepsMonthNameFormat() {

        final AnonymizationService anonymizationService = new DateAnonymizationService();

        for (final String token : new String[]{"Sep 5, 2020", "Sept 5, 2020", "September 5, 2020", "Sep 5 2020", "5 Sep 2020"}) {

            final String replacement = anonymizationService.anonymize(token);

            LOGGER.info("Date: " + token + " -> " + replacement);
            Assertions.assertNotEquals(token, replacement);
            Assertions.assertTrue(MONTH_NAME_FORMAT.matcher(replacement).matches(), token + " was replaced with " + replacement);

        }

    }

    @Test
    public void anonymizeNumericDatesKeepTheirFormat() {

        final AnonymizationService anonymizationService = new DateAnonymizationService();

        Assertions.assertTrue(anonymizationService.anonymize("2018-11-18").matches("\\d{4}-\\d{2}-\\d{2}"));
        Assertions.assertTrue(anonymizationService.anonymize("11-18-2018").matches("\\d{2}-\\d{2}-\\d{4}"));
        Assertions.assertTrue(anonymizationService.anonymize("1-8-2018").matches("\\d{1,2}-\\d{1,2}-\\d{4}"));

    }

    @Test
    public void anonymizeUnknownFormatFallsBackToIsoDate() {

        final AnonymizationService anonymizationService = new DateAnonymizationService();

        Assertions.assertTrue(anonymizationService.anonymize("not a date").matches("\\d{4}-\\d{2}-\\d{2}"));

    }

    @Test
    public void anonymizeUUID() {

        AnonymizationService anonymizationService = new DateAnonymizationService(new SecureRandom(), AnonymizationMethod.UUID);

        final String token = "11-18-2018";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Date: " + replacement);
        Assertions.assertNotEquals(token, replacement);
        Assertions.assertTrue(replacement.length() >= 32);

    }

}
