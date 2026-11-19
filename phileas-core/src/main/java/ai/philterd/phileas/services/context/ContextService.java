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
package ai.philterd.phileas.services.context;

import java.util.function.Supplier;

/**
 * Stores per-context token-to-replacement mappings so a value is anonymized consistently within a context,
 * and values that belong to the context itself, such as a context's random date shift.
 */
public interface ContextService {

    boolean containsToken(final String token);

    boolean containsReplacement(final String replacement);

    String getReplacement(final String token);

    void putReplacement(final String token, final String replacement, final String filterType);

    /**
     * Atomically returns the replacement for a token, generating and storing one via
     * <code>replacementSupplier</code> only if the token has not been seen before. Unlike a
     * separate {@link #containsToken(String)}/{@link #getReplacement(String)}/{@link
     * #putReplacement(String, String, String)} sequence, this is safe under concurrent access:
     * two threads requesting the same token in the same context are guaranteed to receive the
     * same replacement (the supplier is invoked at most once per token), preserving the
     * CONTEXT-scope guarantee that a given token is anonymized consistently.
     *
     * @param token The token to look up or generate a replacement for.
     * @param filterType The {@link ai.philterd.phileas.model.filtering.FilterType} of the token.
     * @param replacementSupplier Supplies a freshly generated replacement when the token is absent.
     * @return The existing or newly generated replacement. Never <code>null</code> unless the
     *         supplier returns <code>null</code>.
     */
    String computeReplacementIfAbsent(final String token, final String filterType, final Supplier<String> replacementSupplier);

    /**
     * Atomically returns a value that belongs to the context rather than to a token, generating and
     * storing one via <code>valueSupplier</code> only if the context has no value for the key. The
     * date filter's random shift with the CONTEXT replacement scope is kept this way, so every date
     * in the context is moved by the same amount. Like {@link #computeReplacementIfAbsent(String,
     * String, Supplier)}, the supplier is invoked at most once per key.
     *
     * <p>The default implementation stores the value as a token replacement through {@link
     * #computeReplacementIfAbsent(String, String, Supplier)}. An implementation that persists its
     * entries, and may evict, delete, or list them, can override this method to store the value
     * with the context, outside its token entries, so the value is not lost with them.</p>
     *
     * @param key The key of the value. A key cannot collide with a detected token.
     * @param filterType The {@link ai.philterd.phileas.model.filtering.FilterType} the value is kept for.
     * @param valueSupplier Supplies a freshly generated value when the context has none for the key.
     * @return The existing or newly generated value. Never <code>null</code> unless the supplier
     *         returns <code>null</code>.
     */
    default String computeValueIfAbsent(final String key, final String filterType, final Supplier<String> valueSupplier) {
        return computeReplacementIfAbsent(key, filterType, valueSupplier);
    }

}
