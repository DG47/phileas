/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.IncrementalRedaction;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

public class IncrementalRedactionsTest {

    private TextFilterResult filter(final String policyJson, final String input) throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("incremental.redactions.enabled", "true");

        final Policy policy = new Gson().fromJson(policyJson, Policy.class);
        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(properties), new DefaultContextService(), new InMemoryVectorService(), null);

        return service.filter(policy, "context", input);

    }

    /**
     * Checks each incremental redaction against its text built the way it was when each one stored
     * its own copy: the replacements so far followed by the rest of the input.
     */
    private void assertMatchesReplacementsAppliedInOrder(final String input, final List<IncrementalRedaction> redactions) {

        final StringBuilder sb = new StringBuilder();
        int copiedTo = 0;

        for(final IncrementalRedaction redaction : redactions) {

            final Span span = redaction.getSpan();
            sb.append(input, copiedTo, span.getCharacterStart()).append(span.getReplacement());
            copiedTo = span.getCharacterEnd();

            final String expected = sb + input.substring(copiedTo);

            Assertions.assertEquals(expected, redaction.getIncrementallyRedactedText());
            Assertions.assertEquals(DigestUtils.sha256Hex(expected), redaction.getHash());

        }

    }

    @Test
    public void incrementalRedactionTextsAndHashes() throws Exception {

        final String input = "SSN 123-45-6789, email jeff@example.com, paid 1.2 USD.";

        final TextFilterResult result = filter(
                "{\"identifiers\": {\"ssn\": {}, \"emailAddress\": {}, \"currency\": {}}}", input);

        Assertions.assertEquals("SSN {{{REDACTED-ssn}}}, email {{{REDACTED-email-address}}}, paid {{{REDACTED-currency}}}.",
                result.getFilteredText());

        final List<IncrementalRedaction> redactions = result.getIncrementalRedactions();
        Assertions.assertEquals(3, redactions.size());

        Assertions.assertEquals("SSN {{{REDACTED-ssn}}}, email jeff@example.com, paid 1.2 USD.",
                redactions.get(0).getIncrementallyRedactedText());
        Assertions.assertEquals("SSN {{{REDACTED-ssn}}}, email {{{REDACTED-email-address}}}, paid 1.2 USD.",
                redactions.get(1).getIncrementallyRedactedText());
        Assertions.assertEquals(result.getFilteredText(), redactions.get(2).getIncrementallyRedactedText());

        // The hashes are those of the texts, as before.
        Assertions.assertEquals(DigestUtils.sha256Hex("SSN {{{REDACTED-ssn}}}, email jeff@example.com, paid 1.2 USD."),
                redactions.get(0).getHash());
        Assertions.assertEquals(DigestUtils.sha256Hex(result.getFilteredText()), redactions.get(2).getHash());

        assertMatchesReplacementsAppliedInOrder(input, redactions);

        // The text is built each time it is asked for and is the same each time.
        Assertions.assertEquals(redactions.get(1).getIncrementallyRedactedText(), redactions.get(1).getIncrementallyRedactedText());

    }

    @Test
    public void incrementalRedactionsDoNotHoldACopyOfTheTextEach() throws Exception {

        final int count = 5_000;
        final String input = "1.2 USD ".repeat(count);

        final long before = usedHeapAfterGc();
        final TextFilterResult result = filter("{\"identifiers\": {\"currency\": {}}}", input);
        final long retained = usedHeapAfterGc() - before;

        final List<IncrementalRedaction> redactions = result.getIncrementalRedactions();
        Assertions.assertEquals(count, redactions.size());

        // A copy of the text for each of the 5,000 redactions would be about 390 MB.
        Assertions.assertTrue(retained < 64L * 1024 * 1024,
                "Filtering retained " + retained / (1024 * 1024) + " MB");

        assertMatchesReplacementsAppliedInOrder(input, redactions);
        Assertions.assertEquals(result.getFilteredText(), redactions.get(count - 1).getIncrementallyRedactedText());

    }

    private static long usedHeapAfterGc() {

        final Runtime runtime = Runtime.getRuntime();

        for(int i = 0; i < 3; i++) {
            System.gc();
        }

        return runtime.totalMemory() - runtime.freeMemory();

    }

}
