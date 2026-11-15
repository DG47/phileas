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
package ai.philterd.phileas.model.filtering;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.Arrays;

/**
 * The replacements applied to one text, in the order they are applied, shared by the
 * {@link IncrementalRedaction incremental redactions} made from them. Each incremental redaction
 * records its position here and builds its text only when asked for it, so the incremental
 * redactions of a text hold the text once rather than a copy for each replacement.
 */
public final class IncrementalRedactionSequence {

    private final String input;
    private int[] starts;
    private int[] ends;
    private String[] replacements;
    private int size;

    /**
     * @param input The text the replacements are applied to.
     * @param expectedReplacements The number of replacements expected, used to size the sequence.
     */
    public IncrementalRedactionSequence(final String input, final int expectedReplacements) {
        this.input = input;
        final int capacity = Math.max(expectedReplacements, 1);
        this.starts = new int[capacity];
        this.ends = new int[capacity];
        this.replacements = new String[capacity];
    }

    /**
     * Applies the span's replacement after the replacements before it.
     * Spans must be added in order of their start, and must not overlap.
     *
     * @param span The span whose replacement is applied.
     * @return The incremental redaction for the text with this and every earlier replacement applied.
     */
    public IncrementalRedaction add(final Span span) {

        if(size > 0 && span.getCharacterStart() < ends[size - 1]) {
            throw new IllegalArgumentException("Spans must be added in order and must not overlap.");
        }

        if(size == starts.length) {
            final int capacity = size * 2;
            starts = Arrays.copyOf(starts, capacity);
            ends = Arrays.copyOf(ends, capacity);
            replacements = Arrays.copyOf(replacements, capacity);
        }

        starts[size] = span.getCharacterStart();
        ends[size] = span.getCharacterEnd();
        replacements[size] = span.getReplacement();

        final int index = size++;

        return new IncrementalRedaction(DigestUtils.sha256Hex(textAfter(index)), span, this, index);

    }

    /**
     * Builds the text with the replacements from the first through the one at the given index applied.
     */
    String textAfter(final int index) {

        final StringBuilder sb = new StringBuilder(input.length());
        int copiedTo = 0;

        for(int i = 0; i <= index; i++) {
            sb.append(input, copiedTo, starts[i]).append(replacements[i]);
            copiedTo = ends[i];
        }

        sb.append(input, copiedTo, input.length());

        return sb.toString();

    }

}
