package hu.blackbelt.judo.meta.asm.runtime;

import static hu.blackbelt.epsilon.runtime.execution.ExecutionContext.executionContextBuilder;
import static hu.blackbelt.epsilon.runtime.execution.contexts.EvlExecutionContext.evlExecutionContextBuilder;
import static hu.blackbelt.epsilon.runtime.execution.model.emf.WrappedEmfModelContext.wrappedEmfModelContextBuilder;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.epsilon.common.util.UriUtil;

import hu.blackbelt.epsilon.runtime.execution.ExecutionContext;
import hu.blackbelt.epsilon.runtime.execution.api.Log;
import hu.blackbelt.epsilon.runtime.execution.exceptions.ScriptExecutionException;

public class AsmEpsilonValidator {
	
	public static void validateAsm(Log log, AsmModel asmModel, URI scriptRoot)
			throws ScriptExecutionException, URISyntaxException {
		validateAsm(log, asmModel, scriptRoot, emptyList(), emptyList());
	}

	public static void validateAsm(Log log, AsmModel asmModel, URI scriptRoot,
			Collection<String> expectedErrors, Collection<String> expectedWarnings)
			throws ScriptExecutionException, URISyntaxException {
		ExecutionContext executionContext = executionContextBuilder().log(log)
				.resourceSet(asmModel.getResourceSet()).metaModels(emptyList())
				.modelContexts(Arrays.asList(wrappedEmfModelContextBuilder().log(log).name("ASM")
						.validateModel(false).resource(asmModel.getResource()).build()))
				.injectContexts(singletonMap("asmUtils", new AsmUtils(asmModel.getResourceSet()))).build();

		try {
            // run the model / metadata loading
			executionContext.load();

            // Transformation script
			executionContext
					.executeProgram(evlExecutionContextBuilder().source(UriUtil.resolve("asm.evl", scriptRoot))
							.expectedErrors(expectedErrors).expectedWarnings(expectedWarnings).build());

		} finally {
			executionContext.commit();
			try {
				executionContext.close();
			} catch (Exception e) {
			}
		}
	}

	public static URI calculateAsmValidationScriptURI() throws URISyntaxException {
		URI asmRoot = AsmModel.class.getProtectionDomain().getCodeSource().getLocation().toURI();
		if (asmRoot.toString().endsWith(".jar")) {
			asmRoot = new URI("jar:" + asmRoot.toString() + "!/validations/");
		} else if (asmRoot.toString().startsWith("jar:bundle:")) {
            // bundle://37.0:0/validations/
            // jar:bundle://37.0:0/!/validations/asm.evl
			asmRoot = new URI(
					asmRoot.toString().substring(4, asmRoot.toString().indexOf("!")) + "validations/");
		} else {
			asmRoot = new URI(asmRoot.toString() + "/validations/");
		}
		return asmRoot;

	}

}
