package dev.runetools.asm.analysis.exception;

import dev.runetools.asm.analysis.value.AbstractValue;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.function.BiPredicate;

/**
 * Validator for {@link ResolvableAnalyzerException}.
 *
 * @author Matt Coley
 */
public interface Validator extends BiPredicate<MethodNode, Frame<AbstractValue>[]> {}
