import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import kotlin.streams.toList
import jcinterpret.document.ConfigDocument
import jcinterpret.document.DocumentUtils
import jcinterpret.parser.FileUtils
import jcinterpret.parser.Parser
import org.eclipse.jdt.core.dom.*
import java.nio.file.Path

data class Project (
    val id: String,
    val path: Path,
    val compilationUnits: List<CompilationUnit>
)

fun main(args: Array<String>) {
    if (args.count() != 1)
        error("One argument is expected listing the path to a valid config document")

    val docPath = Paths.get(args[0])

    if (!Files.exists(docPath) || !Files.isRegularFile(docPath))
        error("The passed argument is not a path to a file")

    val document = DocumentUtils.readJson(docPath, ConfigDocument::class)

    val root = docPath.parent
    val projectsRoot = root.resolve(document.projectsRoot)
    val output = root.resolve(document.output)
        .resolve(projectsRoot.fileName.toString())

    if (!Files.exists(output))
        Files.createDirectories(output)

    if (!Files.exists(projectsRoot))
        throw IllegalArgumentException("Unknown projects root $projectsRoot")

    val globalLibraries = document.globalLibraries.map { root.resolve(it) }
    val projectLibraries = document.projectLibraries?.map { it.key to it.value.map { root.resolve(it) } }?.toMap()
        ?: emptyMap()

    val projectPaths = Files.list(projectsRoot)
        .filter { Files.isDirectory(it) }
        .toList()
        .sorted()

    val dir = output.resolve("${document.title}_${Instant.now().nano}")
    Files.createDirectory(dir)

    //  Pre-Processing: Parse the projects
    println("Pre-Processing: Parsing the projects")
    val projects = projectPaths.mapNotNull { path ->
        val id = path.fileName.toString()
        val sources = FileUtils.listFiles(path, ".java")
        val directories = FileUtils.listDirectories(path)
        val eps = document.entryPoints?.get(id)?.toList() ?: emptyList()
        val libraries = if (projectLibraries.containsKey(id)) {
            globalLibraries + projectLibraries[id]!!
        } else {
            globalLibraries
        }

        val compilationUnits = Parser.parse(sources, libraries, directories)
        val msg = compilationUnits.flatMap { it.messages.toList() }
            .filter { it.message.contains("Syntax error") || it.message.contains("cannot be resolved") }

        if (msg.isNotEmpty()) {
            println("Ignoring $id")
            return@mapNotNull null
        }

        return@mapNotNull Project (
            id,
            path,
            compilationUnits
        )
    }.toList()

    for (project in projects) {

    }
}

class NodeTypeVisitor: ASTVisitor() {
    override fun visit(node: AnnotationTypeDeclaration?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: AnnotationTypeMemberDeclaration?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: AnonymousClassDeclaration?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ArrayAccess?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ArrayCreation?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ArrayInitializer?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ArrayType?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: AssertStatement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: Assignment?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: Block?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: BlockComment?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: BooleanLiteral?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: BreakStatement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: CastExpression?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: CatchClause?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: CharacterLiteral?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ClassInstanceCreation?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: CompilationUnit?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ConditionalExpression?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ConstructorInvocation?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ContinueStatement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: CreationReference?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: Dimension?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: DoStatement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: EmptyStatement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: EnhancedForStatement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: EnumConstantDeclaration?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: EnumDeclaration?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ExportsDirective?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ExpressionMethodReference?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ExpressionStatement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: FieldAccess?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: FieldDeclaration?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ForStatement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: IfStatement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ImportDeclaration?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: InfixExpression?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: Initializer?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: InstanceofExpression?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: IntersectionType?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: Javadoc?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: LabeledStatement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: LambdaExpression?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: LineComment?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: MarkerAnnotation?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: MemberRef?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: MemberValuePair?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: MethodRef?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: MethodRefParameter?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: MethodDeclaration?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: MethodInvocation?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: Modifier?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ModuleDeclaration?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ModuleModifier?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: NameQualifiedType?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: NormalAnnotation?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: NullLiteral?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: NumberLiteral?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: OpensDirective?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: PackageDeclaration?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ParameterizedType?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ParenthesizedExpression?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: PostfixExpression?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: PrefixExpression?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ProvidesDirective?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: PrimitiveType?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: QualifiedName?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: QualifiedType?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: RequiresDirective?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ReturnStatement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: SimpleName?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: SimpleType?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: SingleMemberAnnotation?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: SingleVariableDeclaration?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: StringLiteral?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: SuperConstructorInvocation?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: SuperFieldAccess?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: SuperMethodInvocation?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: SuperMethodReference?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: SwitchCase?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: SwitchStatement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: SynchronizedStatement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: TagElement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: TextElement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ThisExpression?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: ThrowStatement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: TryStatement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: TypeDeclaration?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: TypeDeclarationStatement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: TypeLiteral?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: TypeMethodReference?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: TypeParameter?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: UnionType?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: UsesDirective?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: VariableDeclarationExpression?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: VariableDeclarationStatement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: VariableDeclarationFragment?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: WhileStatement?): Boolean {
        return super.visit(node)
    }

    override fun visit(node: WildcardType?): Boolean {
        return super.visit(node)
    }
}

