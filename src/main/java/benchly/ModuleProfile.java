package benchly;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import modules.InputPort;
import modules.Module;
import modules.Port;

public class ModuleProfile {

	String name;
	
	String description;
	
	Map<String, String> inputPorts;
	
	Map<String, String> outputPorts;
	
	public ModuleProfile(Module module) {
		this.name = module.getName();
		this.description = module.getDescription();
		
		this.inputPorts = portDescriptionsFromPorts(module.getInputPorts().values());
		this.outputPorts = portDescriptionsFromPorts(module.getOutputPorts().values());
	}
	
	// build a number of port descriptions from prot objects
	private static Map<String, String> portDescriptionsFromPorts(Collection<? extends Port> ports) {
		Map<String, String> result = new HashMap<>();
		
		for (Port port : ports) {
			result.put(port.getName(), port.getDescription());
		}
	
		return result;
	}
	
}
