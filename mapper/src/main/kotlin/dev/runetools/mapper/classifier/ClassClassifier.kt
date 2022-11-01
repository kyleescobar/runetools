package dev.runetools.mapper.classifier

import dev.runetools.asm.tree.*
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleWeightedGraph
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

object ClassClassifier : AbstractClassifier<ClassNode>() {

    override fun init() {
        addClassifier(classTypeCheck, 20)
        addClassifier(signature, 5)
        addClassifier(parentClass, 4)
        addClassifier(children, 3)
        addClassifier(interfaces, 3)
        addClassifier(implementers, 2)
        addClassifier(hierarchyDepth, 4)
        addClassifier(methodCount, 3)
        addClassifier(fieldCount, 3)
        addClassifier(strings, 8)
        addClassifier(numbers, 6)
        addClassifier(methodTypeRefs, 8)
        addClassifier(fieldTypeRefs, 8)
        addClassifier(inRefs, 6)
        addClassifier(outRefs, 6)
        addClassifier(methodInRefs, 6)
        addClassifier(methodOutRefs, 5)
        addClassifier(fieldReadRefs, 5)
        addClassifier(fieldWriteRefs, 5)
    }

    private val classTypeCheck = classifier { a, b ->
        val mask = ACC_ENUM or ACC_INTERFACE or ACC_ANNOTATION or ACC_RECORD or ACC_ABSTRACT
        val resultA = a.access and mask
        val resultB = b.access and mask
        return@classifier 1 - Integer.bitCount(resultA xor resultB) / 5.0
    }

    private val signature = classifier { a, b ->
        val sigA = a.type
        val sigB = b.type
        return@classifier if(ClassifierUtil.isMaybeEqual(sigA, sigB)) 1.0 else 0.0
    }

    private val parentClass = classifier { a, b ->
        if(a.parent == null && b.parent == null) return@classifier 1.0
        if(a.parent == null || b.parent == null) return@classifier 0.0
        return@classifier if(ClassifierUtil.isMaybeEqual(a.parent!!, b.parent!!)) 1.0 else 0.0
    }

    private val children = classifier { a, b ->
        return@classifier ClassifierUtil.compareClassSets(a.children, b.children)
    }

    private val interfaces = classifier { a, b ->
        return@classifier ClassifierUtil.compareClassSets(a.interfaceClasses, b.interfaceClasses)
    }

    private val implementers = classifier { a, b ->
        return@classifier ClassifierUtil.compareClassSets(a.implementers, b.implementers)
    }

    private val hierarchyDepth = classifier { a, b ->
        var countA = 0
        var countB = 0

        var clsA = a.parent
        while(clsA != null) {
            clsA = clsA.parent
            countA++
        }

        var clsB = b.parent
        while(clsB != null) {
            clsB = clsB.parent
            countB++
        }

        return@classifier ClassifierUtil.compareCounts(countA, countB)
    }

    private val methodCount = classifier { a, b ->
        return@classifier ClassifierUtil.compareCounts(a.methods.filter { !it.isStatic() }.size, b.methods.filter { !it.isStatic() }.size)
    }

    private val fieldCount = classifier { a, b ->
        return@classifier ClassifierUtil.compareCounts(a.fields.filter { !it.isStatic() }.size, b.fields.filter { !it.isStatic() }.size)
    }

    private val methodTypeRefs = classifier { a, b ->
        return@classifier ClassifierUtil.compareMethodSets(a.methodTypeRefs, b.methodTypeRefs)
    }

    private val fieldTypeRefs = classifier { a, b ->
        return@classifier ClassifierUtil.compareFieldSets(a.fieldTypeRefs, b.fieldTypeRefs)
    }

    private val inRefs = classifier { a, b ->
        val refsA = a.getInRefs()
        val refsB = b.getInRefs()
        return@classifier ClassifierUtil.compareClassSets(refsA, refsB)
    }

    private fun ClassNode.getInRefs(): Set<ClassNode> {
        val ret = hashSetOf<ClassNode>()
        methodTypeRefs.forEach { ret.add(it.owner) }
        fieldTypeRefs.forEach { ret.add(it.owner) }
        return ret
    }

    private val outRefs = classifier { a, b ->
       val refsA = a.getOutRefs()
        val refsB = b.getOutRefs()
        return@classifier ClassifierUtil.compareClassSets(refsA, refsB)
    }

    private fun ClassNode.getOutRefs(): Set<ClassNode> {
        val ret = hashSetOf<ClassNode>()
        methods.filter { !it.isStatic() }.forEach { ret.addAll(it.classRefs) }
        fields.filter { !it.isStatic() }.forEach { pool.findClass(it.type.internalName)?.apply { ret.add(this) } }
        return ret
    }

    private val methodOutRefs = classifier { a, b ->
        val refsA = a.getMethodOutRefs()
        val refsB = b.getMethodOutRefs()
        return@classifier ClassifierUtil.compareMethodSets(refsA, refsB)
    }

    private fun ClassNode.getMethodOutRefs(): Set<MethodNode> {
        val ret = hashSetOf<MethodNode>()
        methods.filter { !it.isStatic() }.forEach { ret.addAll(it.refsOut) }
        return ret
    }

    private val methodInRefs = classifier { a, b ->
        val refsA = a.getMethodInRefs()
        val refsB = b.getMethodInRefs()
        return@classifier ClassifierUtil.compareMethodSets(refsA, refsB)
    }

    private fun ClassNode.getMethodInRefs(): Set<MethodNode> {
        val ret = hashSetOf<MethodNode>()
        methods.filter { !it.isStatic() }.forEach { ret.addAll(it.refsIn) }
        return ret
    }

    private val fieldReadRefs = classifier { a, b ->
        val refsA = a.getFieldReadRefs()
        val refsB = b.getFieldReadRefs()
        return@classifier ClassifierUtil.compareFieldSets(refsA, refsB)
    }

    private fun ClassNode.getFieldReadRefs(): Set<FieldNode> {
        val ret = hashSetOf<FieldNode>()
        methods.filter { !it.isStatic() }.forEach { ret.addAll(it.fieldReadRefs) }
        return ret
    }

    private val fieldWriteRefs = classifier { a, b ->
        val refsA = a.getFieldWriteRefs()
        val refsB = b.getFieldWriteRefs()
        return@classifier ClassifierUtil.compareFieldSets(refsA, refsB)
    }

    private fun ClassNode.getFieldWriteRefs(): Set<FieldNode> {
        val ret = hashSetOf<FieldNode>()
        methods.filter { !it.isStatic() }.forEach { ret.addAll(it.fieldWriteRefs) }
        return ret
    }

    private val strings = classifier { a, b ->
        val stringsA = a.methods.filter { !it.isStatic() }.flatMap { it.strings }.toSet()
        val stringsB = b.methods.filter { !it.isStatic() }.flatMap { it.strings }.toSet()
        return@classifier ClassifierUtil.compareSets(stringsA, stringsB)
    }

    private val numbers = classifier { a, b ->
        val numbersA = a.methods.filter { !it.isStatic() }.flatMap { it.numbers }.toSet()
        val numbersB = b.methods.filter { !it.isStatic() }.flatMap { it.numbers }.toSet()
        return@classifier ClassifierUtil.compareSets(numbersA, numbersB)
    }

    override fun rank(
        fromSet: Set<ClassNode>,
        toSet: Set<ClassNode>,
        filter: (from: ClassNode, to: ClassNode) -> Boolean
    ): List<MatchResult<ClassNode>> {
        val fromNodes = hashSetOf<ClassNode>()
        val toNodes = hashSetOf<ClassNode>()

        fromSet.forEach { from ->
            toSet.forEach { to ->
                if(filter(from, to)) {
                    if(from !in fromNodes) fromNodes.add(from)
                    if(to !in toNodes) toNodes.add(to)
                }
            }
        }

        val graph = SimpleWeightedGraph<ClassNode, DefaultWeightedEdge>(DefaultWeightedEdge::class.java)
        fromNodes.forEach { graph.addVertex(it) }
        toNodes.forEach { graph.addVertex(it) }

        fromNodes.forEach { from ->
            toNodes.forEach { to ->
                var score = 0.0
                classifiers.forEach { classifier ->
                    score += (classifier.calculateScore(from, to) * classifier.weight)
                }

                graph.addEdge(from, to).also {
                    graph.setEdgeWeight(it, score)
                }
            }
        }

        val matching = MaximumWeightBipartiteMatching(graph, fromNodes, toNodes).matching
        return matching.edges.mapNotNull {
            if(matching.isMatched(graph.getEdgeSource(it))) {
                val from = graph.getEdgeSource(it)
                val to = graph.getEdgeTarget(it)
                val weight = graph.getEdgeWeight(it)
                return@mapNotNull MatchResult(from, to, weight)
            } else {
                return@mapNotNull null
            }
        }
    }
}