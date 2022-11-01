package dev.runetools.mapper.classifier

import dev.runetools.asm.tree.*
import dev.runetools.mapper.asm.match
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleWeightedGraph
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodNode
import kotlin.math.max
import kotlin.math.min

object StaticMethodClassifier : AbstractClassifier<MethodNode>() {

    override fun init() {
        addClassifier(methodTypeCheck, 10)
        addClassifier(accessFlags, 4)
        addClassifier(argTypes, 10)
        addClassifier(returnType, 5)
        addClassifier(strings, 5)
        addClassifier(numbers, 5)
        addClassifier(classRefs, 3)
        addClassifier(inRefs, 6)
        addClassifier(outRefs, 6)
        addClassifier(fieldReadRefs, 5)
        addClassifier(fieldWriteRefs, 5)
        addClassifier(lineNumberRange, 8)
        addClassifier(code, 12, ClassifierLevel.SECONDARY)
    }

    private val methodTypeCheck = classifier { a, b ->
        val mask = ACC_STATIC or ACC_NATIVE or ACC_ABSTRACT
        val resultA = a.access and mask
        val resultB = b.access and mask
        return@classifier 1 - Integer.bitCount(resultA xor resultB) / 3.0
    }

    private val accessFlags = classifier { a, b ->
        val mask = (ACC_PUBLIC or ACC_PROTECTED or ACC_PRIVATE) or ACC_FINAL or ACC_SYNCHRONIZED or ACC_BRIDGE or ACC_VARARGS or ACC_STRICT or ACC_SYNTHETIC or ACC_STATIC
        val resultA = a.access and mask
        val resultB = b.access and mask
        return@classifier 1 - Integer.bitCount(resultA xor resultB) / 9.0
    }

    private val argTypes = classifier { a, b ->
        return@classifier ClassifierUtil.compareNodeSets(a.type.argumentTypes.toSet(), b.type.argumentTypes.toSet(), { null }, ClassifierUtil::isMaybeEqual)
    }

    private val returnType = classifier { a, b ->
        return@classifier if(ClassifierUtil.isMaybeEqual(a.type.returnType, b.type.returnType)) 1.0 else 0.0
    }

    private val strings = classifier { a, b ->
        return@classifier ClassifierUtil.compareSets(a.strings, b.strings)
    }

    private val numbers = classifier { a, b ->
        return@classifier ClassifierUtil.compareSets(a.numbers, b.numbers)
    }

    private val classRefs = classifier { a, b ->
        return@classifier ClassifierUtil.compareClassSets(a.classRefs, b.classRefs)
    }

    private val inRefs = classifier { a, b ->
        return@classifier ClassifierUtil.compareMethodSets(a.refsIn, b.refsIn)
    }

    private val outRefs = classifier { a, b ->
        return@classifier ClassifierUtil.compareMethodSets(a.refsOut, b.refsOut)
    }

    private val fieldReadRefs = classifier { a, b ->
        return@classifier ClassifierUtil.compareFieldSets(a.fieldReadRefs, b.fieldReadRefs)
    }

    private val fieldWriteRefs = classifier { a, b ->
        return@classifier ClassifierUtil.compareFieldSets(a.fieldWriteRefs, b.fieldWriteRefs)
    }

    private val lineNumberRange = classifier { a, b ->
        val lineRangeA = a.getLineNumberRange()
        val lineRangeB = b.getLineNumberRange()
        return@classifier ClassifierUtil.compareSets(lineRangeA.toSet(), lineRangeB.toSet())
    }

    private fun MethodNode.getLineNumberRange(): IntRange {
        var min = -1
        var max = -1
        instructions.forEach { insn ->
            if(insn is LineNumberNode) {
                val line = insn.line
                if(min == -1 || line < min) {
                    min = line
                }
                if(max == -1 || line > max) {
                    max = line
                }
            }
        }
        return min..max
    }

    private val code = classifier { a, b ->
        return@classifier ClassifierUtil.compareInsns<AbstractInsnNode>(a, b)
    }

    override fun rank(
        level: ClassifierLevel,
        fromSet: Set<MethodNode>,
        toSet: Set<MethodNode>,
        filter: (from: MethodNode, to: MethodNode) -> Boolean
    ): List<MatchResult<MethodNode>> {
        val fromNodes = hashSetOf<MethodNode>()
        val toNodes = hashSetOf<MethodNode>()

        fromSet.forEach { from ->
            toSet.forEach { to ->
                if(ClassifierUtil.isMaybeEqual(from, to)) {
                    toNodes.add(to)
                }
                if(ClassifierUtil.isMaybeEqual(to, from)) {
                    fromNodes.add(from)
                }
            }
        }

        val graph = SimpleWeightedGraph<MethodNode, DefaultWeightedEdge>(DefaultWeightedEdge::class.java)
        fromNodes.forEach {
            graph.addVertex(it)
        }
        toNodes.forEach {
            graph.addVertex(it)
        }

        fromNodes.forEach { from ->
            toNodes.forEach { to ->
                if(ClassifierUtil.isMaybeEqual(from, to)) {
                    var score = 0.0
                    classifiers[level]!!.forEach { classifier ->
                        score += (classifier.calculateScore(from, to) * classifier.weight)
                    }
                    graph.addEdge(from, to).also { edge ->
                        graph.setEdgeWeight(edge, score)
                    }
                }
            }
        }

        val matching = MaximumWeightBipartiteMatching(graph, fromNodes, toNodes).matching
        return matching.edges.mapNotNull { edge ->
            if(matching.isMatched(matching.graph.getEdgeSource(edge))) {
                val from = matching.graph.getEdgeSource(edge)
                val to = matching.graph.getEdgeTarget(edge)
                val weight = matching.graph.getEdgeWeight(edge)
                return@mapNotNull MatchResult(from, to, weight)
            } else {
                return@mapNotNull null
            }
        }
    }
}