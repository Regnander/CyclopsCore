package org.cyclops.cyclopscore.ingredient.storage;

import org.cyclops.commoncapabilities.api.ingredient.IIngredientMatcher;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorageSlotted;

import java.util.Iterator;
import java.util.function.Predicate;

/**
 * Helper methods for moving ingredients between {@link IIngredientComponentStorage}'s.
 */
public final class IngredientStorageHelpers {

    /**
     * Iteratively move the given maximum quantity of instances from source to destination.
     *
     * This is useful in cases that the internal transfer rate of certain storages have to be overridden.
     *
     * Note: When simulating, only a single iteration will be done.
     * This is because the iterations don't actually take effect,
     * which could cause infinite loops.
     *
     * @param source A source storage to extract from.
     * @param destination A destination storage to insert to.
     * @param maxQuantity The maximum instance quantity to move.
     * @param simulate If the movement should be simulated.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The moved ingredient.
     */
    public static <T, M> T moveIngredientsIterative(IIngredientComponentStorage<T, M> source,
                                                    IIngredientComponentStorage<T, M> destination,
                                                    long maxQuantity, boolean simulate) {
        IngredientComponent<T, M> component = source.getComponent();
        IIngredientMatcher<T, M> matcher = component.getMatcher();
        T movedFirst = moveIngredients(source, destination, maxQuantity, simulate);
        long movedQuantity = matcher.getQuantity(movedFirst);
        if (simulate || movedQuantity == 0) {
            return movedFirst;
        }
        M matchCondition = matcher.getExactMatchNoQuantityCondition();

        // Try move until we reach the max quantity, or we don't move anything anymore.
        while (movedQuantity < maxQuantity) {
            T moved = moveIngredients(source, destination, movedFirst, matchCondition, false);
            if (matcher.isEmpty(moved)) {
                break;
            }
            movedQuantity += matcher.getQuantity(moved);
        }

        return matcher.withQuantity(movedFirst, movedQuantity);
    }

    /**
     * Move the given maximum quantity of instances from source to destination.
     * @param source A source storage to extract from.
     * @param destination A destination storage to insert to.
     * @param maxQuantity The maximum instance quantity to move.
     * @param simulate If the movement should be simulated.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The moved ingredient.
     */
    public static <T, M> T moveIngredients(IIngredientComponentStorage<T, M> source,
                                           IIngredientComponentStorage<T, M> destination,
                                           long maxQuantity, boolean simulate) {
        IIngredientMatcher<T, M> matcher = source.getComponent().getMatcher();
        T extractedSimulated = source.extract(maxQuantity, true);
        long movableQuantity = insertIngredientQuantity(destination, extractedSimulated, true);
        if (movableQuantity > 0) {
            if (simulate) {
                if (maxQuantity == movableQuantity) {
                    return extractedSimulated;
                } else {
                    return matcher.withQuantity(extractedSimulated, movableQuantity);
                }
            } else {
                T extracted = source.extract(movableQuantity, false);
                return insertIngredient(destination, extracted, false);
            }
        }
        return matcher.getEmptyInstance();
    }

    /**
     * Move the first instance that matches the given match condition from source to destination.
     * @param source A source storage to extract from.
     * @param destination A destination storage to insert to.
     * @param instance The prototype instance.
     * @param matchCondition The match condition.
     * @param simulate If the movement should be simulated.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The moved ingredient.
     */
    public static <T, M> T moveIngredients(IIngredientComponentStorage<T, M> source,
                                           IIngredientComponentStorage<T, M> destination,
                                           T instance, M matchCondition, boolean simulate) {
        IIngredientMatcher<T, M> matcher = source.getComponent().getMatcher();
        Iterator<T> it = source.iterator(instance, matcher.withoutCondition(matchCondition,
                source.getComponent().getPrimaryQuantifier().getMatchCondition()));
        long prototypeQuantity = matcher.getQuantity(instance);
        while (it.hasNext()) {
            T extractedSimulated = it.next();
            if (matcher.getQuantity(extractedSimulated) != prototypeQuantity) {
                extractedSimulated = matcher.withQuantity(extractedSimulated, prototypeQuantity);
            }
            T moved = moveIngredient(source, destination, extractedSimulated, matchCondition, simulate);
            if (!matcher.isEmpty(moved)) {
                return moved;
            }
        }
        return matcher.getEmptyInstance();
    }

    /**
     * Move the first instance that matches the given predicate from source to destination.
     * @param source A source storage to extract from.
     * @param destination A destination storage to insert to.
     * @param predicate The predicate to match instances by.
     * @param maxQuantity The max quantity that can be moved.
     * @param exactQuantity If the max quantity should be interpreted as an exact quantity.
     * @param simulate If the movement should be simulated.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The moved ingredient.
     */
    public static <T, M> T moveIngredients(IIngredientComponentStorage<T, M> source,
                                           IIngredientComponentStorage<T, M> destination,
                                           Predicate<T> predicate, long maxQuantity, boolean exactQuantity,
                                           boolean simulate) {
        IIngredientMatcher<T, M> matcher = source.getComponent().getMatcher();
        for (T extractedSimulated : source) {
            if (predicate.test(extractedSimulated)) {
                if (matcher.getQuantity(extractedSimulated) > maxQuantity) {
                    extractedSimulated = matcher.withQuantity(extractedSimulated, maxQuantity);
                }
                extractedSimulated = source.extract(extractedSimulated, matcher.getExactMatchNoQuantityCondition(), true);
                if (!matcher.isEmpty(extractedSimulated)) {
                    T movable = insertIngredient(destination, extractedSimulated, true);
                    if (!matcher.isEmpty(movable)
                            && (exactQuantity ? matcher.getQuantity(movable) == maxQuantity
                            : matcher.getQuantity(movable) <= maxQuantity)) {
                        if (simulate) {
                            return movable;
                        } else {
                            T extracted = source.extract(movable, matcher.getExactMatchNoQuantityCondition(), false);
                            return insertIngredient(destination, extracted, false);
                        }
                    }
                }
            }
        }
        return matcher.getEmptyInstance();
    }

    /**
     * Move ingredients from source to target with optional source and target slots,
     * based on an ingredient prototype and match condition.
     *
     * If the algorithm should iterate over all source/destination slot,
     * then the respective slot should be -1.
     *
     * If a slot is defined, and the storage is not an instance of {@link IIngredientComponentStorageSlotted},
     * then nothing will be moved.
     *
     * @param source A source storage to extract from.
     * @param sourceSlot The source slot or -1 for any.
     * @param destination A destination storage to insert to.
     * @param destinationSlot The destination slot or -1 for any.
     * @param instance The prototype instance.
     * @param matchCondition The match condition.
     * @param simulate If the movement should be simulated.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The moved ingredient.
     */
    public static <T, M> T moveIngredientsSlotted(IIngredientComponentStorage<T, M> source, int sourceSlot,
                                                  IIngredientComponentStorage<T, M> destination, int destinationSlot,
                                                  T instance, M matchCondition, boolean simulate) {
        IIngredientMatcher<T, M> matcher = source.getComponent().getMatcher();
        boolean loopSourceSlots = sourceSlot < 0;
        boolean loopDestinationSlots = destinationSlot < 0;

        if (!loopSourceSlots && !loopDestinationSlots) {
            // Both source and destination slot are defined

            // Fail if source or destination are not slotted
            if (!(source instanceof IIngredientComponentStorageSlotted)) {
                return matcher.getEmptyInstance();
            }
            if (!(destination instanceof IIngredientComponentStorageSlotted)) {
                return matcher.getEmptyInstance();
            }
            IIngredientComponentStorageSlotted<T, M> sourceSlotted = (IIngredientComponentStorageSlotted<T, M>) source;
            IIngredientComponentStorageSlotted<T, M> destinationSlotted = (IIngredientComponentStorageSlotted<T, M>) destination;

            // Extract from source slot (simulated)
            long prototypeQuantity = matcher.getQuantity(instance);
            T extractedSimulated = sourceSlotted.extract(sourceSlot, prototypeQuantity, true);
            if (!matcher.isEmpty(extractedSimulated) && matcher.matches(instance, extractedSimulated, matchCondition)) {
                // Insert into target slot  (simulated)
                T remaining = destinationSlotted.insert(destinationSlot, extractedSimulated, true);
                long remainingQuantity = matcher.getQuantity(remaining);
                if (remainingQuantity == 0 ||
                        (remainingQuantity < prototypeQuantity && !matcher.hasCondition(matchCondition,
                                source.getComponent().getPrimaryQuantifier().getMatchCondition()))) {
                    if (simulate) {
                        // Return the result if we intended to simulate
                        if (remainingQuantity == 0) {
                            return extractedSimulated;
                        } else {
                            return matcher.withQuantity(extractedSimulated,
                                    matcher.getQuantity(extractedSimulated) - matcher.getQuantity(remaining));
                        }
                    } else {
                        // Redo the operation if we do not intend to simulate
                        long movedQuantitySimulated = matcher.getQuantity(extractedSimulated) - matcher.getQuantity(remaining);
                        T sourceInstanceEffective = sourceSlotted.extract(sourceSlot, movedQuantitySimulated, false);
                        // The following should always be true. If not, then the source was lying during simulated mode.
                        // But we can safely ignore those cases at this point as nothing has been moved yet.
                        if (!matcher.isEmpty(sourceInstanceEffective)) {
                            // Remaining should be empty, otherwise the destination was lying during simulated mode
                            T remainingEffective = destinationSlotted.insert(destinationSlot, sourceInstanceEffective, false);
                            if (matcher.isEmpty(remainingEffective)) {
                                return sourceInstanceEffective;
                            } else {
                                // If the destination was lying, try to add the remainder back into the source.
                                // If even that fails, throw an error.
                                T remainderFixup = sourceSlotted.insert(sourceSlot, remainingEffective, false);
                                if (matcher.isEmpty(remainderFixup)) {
                                    // We've managed to fix the problem, calculate the effective instance that was moved.
                                    return matcher.withQuantity(remainingEffective,
                                            matcher.getQuantity(sourceInstanceEffective)
                                                    - matcher.getQuantity(remainingEffective));
                                } else {
                                    throw new IllegalStateException("Slotted source to destination movement failed " +
                                            "due to inconsistent insertion behaviour by destination in simulation " +
                                            "and non-simulation: " + destination + ". Lost: " + remainderFixup);
                                }
                            }
                        }
                    }
                }
            }
        } else if (loopSourceSlots) {
            if (source instanceof IIngredientComponentStorageSlotted) {
                // Recursively call movement logic for each slot in the source if slotted.
                IIngredientComponentStorageSlotted<T, M> sourceSlotted = (IIngredientComponentStorageSlotted<T, M>) source;
                int slots = sourceSlotted.getSlots();
                for (int slot = 0; slot < slots; slot++) {
                    T moved = moveIngredientsSlotted(source, slot, destination, destinationSlot, instance, matchCondition, simulate);
                    if (!matcher.isEmpty(moved)) {
                        return moved;
                    }
                }
            } else {
                // If we don't have source slots, iterate over all source slot instances in a slotless way
                long prototypeQuantity = matcher.getQuantity(instance);
                if (loopDestinationSlots) {
                    return moveIngredients(source, destination, instance, matchCondition, simulate);
                } else {
                    if (!(destination instanceof IIngredientComponentStorageSlotted)) {
                        return matcher.getEmptyInstance();
                    }
                    IIngredientComponentStorageSlotted<T, M> destinationSlotted = (IIngredientComponentStorageSlotted<T, M>) destination;
                    for (T sourceInstance : source) {
                        if (matcher.matches(instance, sourceInstance, matcher.withoutCondition(matchCondition,
                                source.getComponent().getPrimaryQuantifier().getMatchCondition()))) {
                            if (matcher.getQuantity(sourceInstance) != prototypeQuantity) {
                                sourceInstance = matcher.withQuantity(sourceInstance, prototypeQuantity);
                            }
                            T extractedSimulated = source.extract(sourceInstance, matchCondition, true);
                            if (!matcher.isEmpty(extractedSimulated)) {
                                T remaining = destinationSlotted.insert(destinationSlot, extractedSimulated, true);
                                long remainingQuantity = matcher.getQuantity(remaining);
                                if (remainingQuantity == 0 ||
                                        (remainingQuantity < prototypeQuantity && !matcher.hasCondition(matchCondition,
                                                source.getComponent().getPrimaryQuantifier().getMatchCondition()))) {
                                    // Set the movable instance
                                    T shouldMove;
                                    if (remainingQuantity == 0) {
                                        shouldMove = extractedSimulated;
                                    } else {
                                        shouldMove = matcher.withQuantity(extractedSimulated,
                                                matcher.getQuantity(extractedSimulated) - matcher.getQuantity(remaining));
                                    }

                                    if (simulate) {
                                        // Return the result if we intended to simulate
                                        return shouldMove;
                                    } else {
                                        T extractedEffective = source.extract(shouldMove, matchCondition, false);
                                        // The following should always be true. If not, then the source was lying during simulated mode.
                                        // But we can safely ignore those cases at this point as nothing has been moved yet.
                                        if (!matcher.isEmpty(extractedSimulated)) {
                                            // Remaining should be empty, otherwise the destination was lying during simulated mode
                                            T remainingEffective = destinationSlotted.insert(destinationSlot, extractedEffective, false);
                                            boolean remainingEffectiveEmpty = matcher.isEmpty(remainingEffective);
                                            if (remainingEffectiveEmpty) {
                                                return extractedEffective;
                                            } else {
                                                // If the destination was lying, try to add the remainder back into the source.
                                                // If even that fails, throw an error.
                                                T remainderFixup = source.insert(remainingEffective, false);
                                                if (matcher.isEmpty(remainderFixup)) {
                                                    // We've managed to fix the problem, calculate the effective instance that was moved.
                                                    return matcher.withQuantity(remainingEffective,
                                                            matcher.getQuantity(extractedEffective)
                                                                    - matcher.getQuantity(remainingEffective));
                                                } else {
                                                    throw new IllegalStateException("Slotless source to destination movement failed " +
                                                            "due to inconsistent insertion behaviour by destination in simulation " +
                                                            "and non-simulation: " + destination + ". Lost: " + remainderFixup);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else { // loopDestinationSlots && !loopSourceSlots
            // Quickly break if the source is not slotted.
            if (!(source instanceof IIngredientComponentStorageSlotted)) {
                return matcher.getEmptyInstance();
            }

            if (destination instanceof IIngredientComponentStorageSlotted) {
                // Recursively call movement logic for each slot in the destination if slotted.
                IIngredientComponentStorageSlotted<T, M> destinationSlotted = (IIngredientComponentStorageSlotted<T, M>) destination;
                int slots = destinationSlotted.getSlots();
                for (int slot = 0; slot < slots; slot++) {
                    T moved = moveIngredientsSlotted(source, sourceSlot, destination, slot, instance, matchCondition, simulate);
                    if (!matcher.isEmpty(moved)) {
                        return moved;
                    }
                }
            } else {
                // If we don't have destination slots, move from defined source slot
                IIngredientComponentStorageSlotted<T, M> sourceSlotted = (IIngredientComponentStorageSlotted<T, M>) source;
                long prototypeQuantity = matcher.getQuantity(instance);
                T sourceInstance = sourceSlotted.extract(sourceSlot, prototypeQuantity, true);
                if (!matcher.isEmpty(sourceInstance) && matcher.matches(instance, sourceInstance, matchCondition)) {
                    T inserted = insertIngredient(destination, sourceInstance, true);
                    if (simulate) {
                        return inserted;
                    } else if (!matcher.isEmpty(inserted)) {
                        T sourceInstanceEffective = sourceSlotted.extract(sourceSlot, matcher.getQuantity(inserted), false);
                        return insertIngredient(destination, sourceInstanceEffective, false);
                    }
                }
            }
        }
        return matcher.getEmptyInstance();
    }

    /**
     * Move ingredients from source to target with optional source and target slots,
     * based on an ingredient predicate.
     *
     * If the algorithm should iterate over all source/destination slot,
     * then the respective slot should be -1.
     *
     * If a slot is defined, and the storage is not an instance of {@link IIngredientComponentStorageSlotted},
     * then nothing will be moved.
     *
     * @param source A source storage to extract from.
     * @param sourceSlot The source slot or -1 for any.
     * @param destination A destination storage to insert to.
     * @param destinationSlot The destination slot or -1 for any.
     * @param predicate The instance predicate.
     * @param maxQuantity The max quantity that can be moved.
     * @param exactQuantity If the max quantity should be interpreted as an exact quantity.
     * @param simulate If the movement should be simulated.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The moved ingredient.
     */
    public static <T, M> T moveIngredientsSlotted(IIngredientComponentStorage<T, M> source, int sourceSlot,
                                                  IIngredientComponentStorage<T, M> destination, int destinationSlot,
                                                  Predicate<T> predicate, long maxQuantity, boolean exactQuantity,
                                                  boolean simulate) {
        IIngredientMatcher<T, M> matcher = source.getComponent().getMatcher();
        boolean loopSourceSlots = sourceSlot < 0;
        boolean loopDestinationSlots = destinationSlot < 0;

        if (!loopSourceSlots && !loopDestinationSlots) {
            // Both source and destination slot are defined

            // Fail if source or destination are not slotted
            if (!(source instanceof IIngredientComponentStorageSlotted)) {
                return matcher.getEmptyInstance();
            }
            if (!(destination instanceof IIngredientComponentStorageSlotted)) {
                return matcher.getEmptyInstance();
            }
            IIngredientComponentStorageSlotted<T, M> sourceSlotted = (IIngredientComponentStorageSlotted<T, M>) source;
            IIngredientComponentStorageSlotted<T, M> destinationSlotted = (IIngredientComponentStorageSlotted<T, M>) destination;

            // Extract from source slot (simulated)
            T extractedSimulated = sourceSlotted.extract(sourceSlot, maxQuantity, true);
            if (!matcher.isEmpty(extractedSimulated) && predicate.test(extractedSimulated)
                    && (!exactQuantity || matcher.getQuantity(extractedSimulated) == maxQuantity)) {
                // Insert into target slot  (simulated)
                T remaining = destinationSlotted.insert(destinationSlot, extractedSimulated, true);
                long remainingQuantity = matcher.getQuantity(remaining);
                if (remainingQuantity == 0 || (remainingQuantity < maxQuantity && !exactQuantity)) {
                    if (simulate) {
                        // Return the result if we intended to simulate
                        if (remainingQuantity == 0) {
                            return extractedSimulated;
                        } else {
                            return matcher.withQuantity(extractedSimulated,
                                    matcher.getQuantity(extractedSimulated) - matcher.getQuantity(remaining));
                        }
                    } else {
                        // Redo the operation if we do not intend to simulate
                        long movedQuantitySimulated = matcher.getQuantity(extractedSimulated) - matcher.getQuantity(remaining);
                        T sourceInstanceEffective = sourceSlotted.extract(sourceSlot, movedQuantitySimulated, false);
                        // The following should always be true. If not, then the source was lying during simulated mode.
                        // But we can safely ignore those cases at this point as nothing has been moved yet.
                        if (!matcher.isEmpty(sourceInstanceEffective)) {
                            // Remaining should be empty, otherwise the destination was lying during simulated mode
                            T remainingEffective = destinationSlotted.insert(destinationSlot, sourceInstanceEffective, false);
                            if (matcher.isEmpty(remainingEffective)) {
                                return sourceInstanceEffective;
                            } else {
                                // If the destination was lying, try to add the remainder back into the source.
                                // If even that fails, throw an error.
                                T remainderFixup = sourceSlotted.insert(sourceSlot, remainingEffective, false);
                                if (matcher.isEmpty(remainderFixup)) {
                                    // We've managed to fix the problem, calculate the effective instance that was moved.
                                    return matcher.withQuantity(remainingEffective,
                                            matcher.getQuantity(sourceInstanceEffective)
                                                    - matcher.getQuantity(remainingEffective));
                                } else {
                                    throw new IllegalStateException("Slotted source to destination movement failed " +
                                            "due to inconsistent insertion behaviour by destination in simulation " +
                                            "and non-simulation: " + destination + ". Lost: " + remainderFixup);
                                }
                            }
                        }
                    }
                }
            }
        } else if (loopSourceSlots) {
            if (source instanceof IIngredientComponentStorageSlotted) {
                // Recursively call movement logic for each slot in the source if slotted.
                IIngredientComponentStorageSlotted<T, M> sourceSlotted = (IIngredientComponentStorageSlotted<T, M>) source;
                int slots = sourceSlotted.getSlots();
                for (int slot = 0; slot < slots; slot++) {
                    T moved = moveIngredientsSlotted(source, slot, destination, destinationSlot, predicate, maxQuantity, exactQuantity, simulate);
                    if (!matcher.isEmpty(moved)) {
                        return moved;
                    }
                }
            } else {
                // If we don't have source slots, iterate over all source slot instances in a slotless way
                if (loopDestinationSlots) {
                    return moveIngredients(source, destination, predicate, maxQuantity, exactQuantity, simulate);
                } else {
                    if (!(destination instanceof IIngredientComponentStorageSlotted)) {
                        return matcher.getEmptyInstance();
                    }
                    IIngredientComponentStorageSlotted<T, M> destinationSlotted = (IIngredientComponentStorageSlotted<T, M>) destination;
                    for (T sourceInstance : source) {
                        if (predicate.test(sourceInstance)) {
                            if (matcher.getQuantity(sourceInstance) != maxQuantity) {
                                sourceInstance = matcher.withQuantity(sourceInstance, maxQuantity);
                            }
                            T extractedSimulated = source.extract(sourceInstance, matcher.getExactMatchCondition(), true);
                            if (!matcher.isEmpty(extractedSimulated)) {
                                T remaining = destinationSlotted.insert(destinationSlot, extractedSimulated, true);
                                long remainingQuantity = matcher.getQuantity(remaining);
                                if (remainingQuantity == 0 || (remainingQuantity < maxQuantity && !exactQuantity)) {
                                    // Set the movable instance
                                    T shouldMove;
                                    if (remainingQuantity == 0) {
                                        shouldMove = extractedSimulated;
                                    } else {
                                        shouldMove = matcher.withQuantity(extractedSimulated,
                                                matcher.getQuantity(extractedSimulated) - matcher.getQuantity(remaining));
                                    }

                                    if (simulate) {
                                        // Return the result if we intended to simulate
                                        return shouldMove;
                                    } else {
                                        T extractedEffective = source.extract(shouldMove, matcher.getExactMatchCondition(), false);
                                        // The following should always be true. If not, then the source was lying during simulated mode.
                                        // But we can safely ignore those cases at this point as nothing has been moved yet.
                                        if (!matcher.isEmpty(extractedSimulated)) {
                                            // Remaining should be empty, otherwise the destination was lying during simulated mode
                                            T remainingEffective = destinationSlotted.insert(destinationSlot, extractedEffective, false);
                                            boolean remainingEffectiveEmpty = matcher.isEmpty(remainingEffective);
                                            if (remainingEffectiveEmpty) {
                                                return extractedEffective;
                                            } else {
                                                // If the destination was lying, try to add the remainder back into the source.
                                                // If even that fails, throw an error.
                                                T remainderFixup = source.insert(remainingEffective, false);
                                                if (matcher.isEmpty(remainderFixup)) {
                                                    // We've managed to fix the problem, calculate the effective instance that was moved.
                                                    return matcher.withQuantity(remainingEffective,
                                                            matcher.getQuantity(extractedEffective)
                                                                    - matcher.getQuantity(remainingEffective));
                                                } else {
                                                    throw new IllegalStateException("Slotless source to destination movement failed " +
                                                            "due to inconsistent insertion behaviour by destination in simulation " +
                                                            "and non-simulation: " + destination + ". Lost: " + remainderFixup);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else { // loopDestinationSlots && !loopSourceSlots
            // Quickly break if the source is not slotted.
            if (!(source instanceof IIngredientComponentStorageSlotted)) {
                return matcher.getEmptyInstance();
            }

            if (destination instanceof IIngredientComponentStorageSlotted) {
                // Recursively call movement logic for each slot in the destination if slotted.
                IIngredientComponentStorageSlotted<T, M> destinationSlotted = (IIngredientComponentStorageSlotted<T, M>) destination;
                int slots = destinationSlotted.getSlots();
                for (int slot = 0; slot < slots; slot++) {
                    T moved = moveIngredientsSlotted(source, sourceSlot, destination, slot, predicate, maxQuantity, exactQuantity, simulate);
                    if (!matcher.isEmpty(moved)) {
                        return moved;
                    }
                }
            } else {
                // If we don't have destination slots, move from defined source slot
                IIngredientComponentStorageSlotted<T, M> sourceSlotted = (IIngredientComponentStorageSlotted<T, M>) source;
                T sourceInstance = sourceSlotted.extract(sourceSlot, maxQuantity, true);
                if (!matcher.isEmpty(sourceInstance) && predicate.test(sourceInstance)) {
                    T inserted = insertIngredient(destination, sourceInstance, true);
                    if (exactQuantity && matcher.getQuantity(inserted) != maxQuantity) {
                        return matcher.getEmptyInstance();
                    }
                    if (simulate) {
                        return inserted;
                    } else if (!matcher.isEmpty(inserted)) {
                        T sourceInstanceEffective = sourceSlotted.extract(sourceSlot, matcher.getQuantity(inserted), false);
                        return insertIngredient(destination, sourceInstanceEffective, false);
                    }
                }
            }
        }
        return matcher.getEmptyInstance();
    }

    /**
     * Move the first instance that matches the given match condition from source to destination.
     *
     * The main difference of this method to
     * {@link #moveIngredients(IIngredientComponentStorage, IIngredientComponentStorage, Object, Object, boolean)}
     * is that the latter method will try checking *multiple* ingredients from the source,
     * while this method will only check the *first matching* ingredient.
     * This makes this method potentially more efficient than the latter.
     *
     * @param source A source storage to extract from.
     * @param destination A destination storage to insert to.
     * @param instance The prototype instance.
     * @param matchCondition The match condition.
     * @param simulate If the movement should be simulated.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The moved ingredient.
     */
    public static <T, M> T moveIngredient(IIngredientComponentStorage<T, M> source,
                                          IIngredientComponentStorage<T, M> destination,
                                          T instance, M matchCondition, boolean simulate) {
        IIngredientMatcher<T, M> matcher = source.getComponent().getMatcher();
        T extractedSimulated = source.extract(instance, matchCondition, true);
        if (!matcher.isEmpty(extractedSimulated)) {
            long movableQuantity = insertIngredientQuantity(destination, extractedSimulated, true);
            if (movableQuantity > 0) {
                if (simulate) {
                    if (matcher.getQuantity(instance) == movableQuantity) {
                        return extractedSimulated;
                    } else {
                        return matcher.withQuantity(extractedSimulated, movableQuantity);
                    }
                } else {
                    T extracted = source.extract(instance, matchCondition, false);
                    return insertIngredient(destination, extracted, false);
                }
            }
        }
        return matcher.getEmptyInstance();
    }

    /**
     * Insert an ingredient in a destination storage.
     *
     * The difference of this method compared to {@link IIngredientComponentStorage#insert(Object, boolean)}
     * is that this method returns the actually inserted ingredient quantity
     * instead of the remaining ingredient that was not inserted.
     *
     * @param destination A storage.
     * @param instance An instance to insert.
     * @param simulate If the insertion should be simulated.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The actual inserted ingredient quantity, or would-be inserted ingredient quantity if simulated.
     */
    public static <T, M> long insertIngredientQuantity(IIngredientComponentStorage<T, M> destination,
                                                       T instance, boolean simulate) {
        IIngredientMatcher<T, M> matcher = destination.getComponent().getMatcher();
        long quantity = matcher.getQuantity(instance);
        if (quantity > 0) {
            T remainingInserted = destination.insert(instance, simulate);
            long remainingInsertedQuantity = matcher.getQuantity(remainingInserted);
            return quantity - remainingInsertedQuantity;
        }
        return 0;
    }

    /**
     * Insert an ingredient in a destination storage.
     *
     * The difference of this method compared to {@link IIngredientComponentStorage#insert(Object, boolean)}
     * is that this method returns the actually inserted ingredient
     * instead of the remaining ingredient that was not inserted.
     *
     * @param destination A storage.
     * @param instance An instance to insert.
     * @param simulate If the insertion should be simulated.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The actual inserted ingredient, or would-be inserted ingredient if simulated.
     */
    public static <T, M> T insertIngredient(IIngredientComponentStorage<T, M> destination,
                                            T instance, boolean simulate) {
        IIngredientMatcher<T, M> matcher = destination.getComponent().getMatcher();
        return matcher.withQuantity(instance, insertIngredientQuantity(destination, instance, simulate));
    }

}
