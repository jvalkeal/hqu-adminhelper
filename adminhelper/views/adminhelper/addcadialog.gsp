

	<table>
		<tr>
			<td><label for="name">${l.cadialoglrestype}: </label></td>
			<td>
				<select name="appdefTypeKey" id="appdefTypeKey" onchange="fillResources(this)">
				<option value="-1" disabled="disabled" class="boldText">Server Type</option>
				<%
				servers.each{k,v ->
					out.write('<option value="' + v + '">' + k + '</option>')
				}
				%>			
				<option value="-1" disabled="disabled" class="boldText">Service Type</option>
				<%
				services.each{k,v ->
					out.write('<option value="' + v + '">' + k + '</option>')
				}
				%>	
				</select>
			</td>
		</tr>
		<tr>
			<td><label for="name">${l.cadialoglresname}: </label></td>
			<td><span id="resourceSelect"></span></td>
		</tr>
		<tr>
			<td><label for="name">${l.cadialoglcontroltype}: </label></td>
			<td><span id="actionSelect"></span></td>
		</tr>		
		<tr>
			<td colspan="2" align="center">
				<button dojo11Type=dijit11.form.Button type="submit">${l.cadialoglok}</button>
				
				
			</td>
		</tr>
	</table>