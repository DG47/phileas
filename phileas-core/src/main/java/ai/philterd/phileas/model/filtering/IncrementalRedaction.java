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
package ai.philterd.phileas.model.filtering;

public class IncrementalRedaction {

    private final String hash;
    private final Span span;

    // Either the text itself, or the sequence of replacements it is built from and this redaction's place in it.
    private final String incrementallyRedactedText;
    private final transient IncrementalRedactionSequence sequence;
    private final int index;

    public IncrementalRedaction(final String hash, final Span span, final String incrementallyRedactedText) {
        this.hash = hash;
        this.span = span;
        this.incrementallyRedactedText = incrementallyRedactedText;
        this.sequence = null;
        this.index = -1;
    }

    IncrementalRedaction(final String hash, final Span span, final IncrementalRedactionSequence sequence, final int index) {
        this.hash = hash;
        this.span = span;
        this.incrementallyRedactedText = null;
        this.sequence = sequence;
        this.index = index;
    }

    @Override
    public String toString() {
        return "IncrementalRedaction{" +
                "hash='" + hash + '\'' +
                ", span=" + span +
                ", incrementallyRedactedText='" + getIncrementallyRedactedText() + '\'' +
                '}';
    }

    public String getHash() {
        return hash;
    }

    public Span getSpan() {
        return span;
    }

    /**
     * Gets the text with this redaction's replacement and every replacement before it applied.
     * When the redaction was made by an {@link IncrementalRedactionSequence}, the text is built
     * on each call rather than stored.
     */
    public String getIncrementallyRedactedText() {
        if(sequence != null) {
            return sequence.textAfter(index);
        }
        return incrementallyRedactedText;
    }

}
