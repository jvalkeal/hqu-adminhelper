/**
 * This is helper class to store information for status of cloning.
 * During process of cloning several alert definitions,
 * progress is stored to this class.
 */
class Report {
	
	 /** statuses */
	public final static int OK = 0
	public final static int WARN = 1
	public final static int ERROR = 2
	
	/**  container for individual reports */
	def reports = []
	
	/** 
	 * Storing info for report caller so we
	 * can provide different outputs.
	 */
	private int instance
	public final static int INSTANCE_SYNC = 1
	public final static int INSTANCE_CLONE = 2
	
	/**
	 * Constructor.
	 * @param instance Type of the reporter. (sync or clone)
	 */
	def Report(int instance) {
		this.instance = instance
	}
	
	/**
	 * This method is creating cloning status report text.
	 * Text is formatted as html text, since it's
	 * used within popup window in hqu plugin.
	 * 
	 * @return Message string
	 */
	def getStatusAsString() {
		def ok = 0
		def err = 0
		def warn = 0
		def i = 1
		reports.each{
			if(it.status == Report.OK)
				ok++
			else if (it.status == Report.WARN)
				warn++
			else if (it.status == Report.ERROR)
				err++
		}
		def s = ''
		if(instance == INSTANCE_CLONE)
			s += "<div class=\"reportstatus\">${ok} alert(s) created. ${err} alert(s) failed. ${warn} alert(s) with warnings.</div>"
		else if(instance == INSTANCE_SYNC)
			s += "<div class=\"reportstatus\">${ok} alert(s) synchronized. ${err} alert(s) failed. ${warn} alert(s) with warnings.</div>"
		
			
		reports.each{
			if(instance == INSTANCE_CLONE)
				s += "<div class=\"reportheader\">Clone ${i}</div>"
			else if(instance == INSTANCE_SYNC)
				s += "<div class=\"reportheader\">Sync ${i}</div>"
			def statstr = levelAsString(it.status)
			s += "<div class=\"reportsection\">Status: ${statstr}</div>"
			it.message.each {m,l ->
				s += "<div class=\"reportsection\">Reason: ${m}</div>" 
			}
			i++
		}
		s
	}
	
	/**
	 * Adding report item to reports list
	 * @param r ReportItem
	 */
	def addReport(ReportItem r) {
		reports << r
	}
	
	/**
	 * Return status level as string.
	 * @param i Status level as int
	 */
	def levelAsString(i) {
		def s = "UNDEFINED"
			if(i == Report.OK)
				s = "OK"
			else if (i == Report.WARN)
				s = "WARN"
			else if (i == Report.ERROR)
				s = "ERROR"		
		s
	}
	
	/**
	 * This method will check if clone status has errors.
	 * 
	 * @return Return true if no errors occured during cloning operation.
	 */
	def isOK() {
		boolean ret = true
		reports.each{
			if (it.status != Report.OK)
				ret = false
		}
		ret
	}
	
}

/**
 * In java this would be inner class, but since we are in groovy it's not.
 * We are using this class to store status of individual clone operations.
 * One instance of this class is stored to CloneStatus object with every
 * cloned alert definition.
 */
class ReportItem {
	def status = 0
	def message = [:]
	
	/**
	 * Default constructor.
	 */
	ReportItem() {
	}
	
	/**
	 * Adding message to array.
	 * @param m Message to add to array.
	 * @param l Level off this message
	 */
	def addMessage(m,l) {
		if (l > status)
			status = l
		message.put(m,l)
	}
	
	/**
	 * Adding message to array with OK status.
	 * @param m Message to add to array.
	 */
	def addMessage(m) {
		message.put(m,Report.OK)
	}

}