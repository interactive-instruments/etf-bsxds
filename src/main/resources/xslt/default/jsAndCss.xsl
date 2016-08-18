<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:x="ii.exclude">
	
	<xsl:import href="lang/current.xsl"/>
	<xsl:variable name="lang" select="document('lang/current.xsl')/*/x:lang"/>
	
	<xsl:output method="html" indent="yes" encoding="UTF-8"/>
	
	<xsl:variable name="defaultStyleResourcePath">
		<xsl:text>http://services.interactive-instruments.de/etf/css</xsl:text>
	</xsl:variable>
	<xsl:param name="stylePath" select="$defaultStyleResourcePath"/>
	
	<!-- JQuery Mobile and Styling includes-->
	<!-- ########################################################################################## -->
	<xsl:template name="jsfdeclAndCss">
		<meta charset="utf-8"/>
		<link rel="stylesheet" href="{$stylePath}/de.interactive-instruments.min.css"/>
		<link rel="stylesheet" href="{$stylePath}/de.interactive-instruments.rep.css"/>
		
		<link rel="stylesheet" href="http://ajax.googleapis.com/ajax/libs/jquerymobile/1.4.5/jquery.mobile.min.css" />
		<script src="http://ajax.aspnetcdn.com/ajax/jQuery/jquery-1.11.3.min.js"></script>
		<script src="http://ajax.googleapis.com/ajax/libs/jquerymobile/1.4.5/jquery.mobile.js"></script>
		
		<!--
		<link rel="stylesheet" href="{$jqueryPath}/jquery.mobile.icons.min.css"/>
		<link rel="stylesheet" href="{$jqueryPath}/jquery.mobile.structure.min.css"/>
		<script src="{$jqueryPath}/jquery.min.js"/>
		<script src="{$jqueryPath}/jquery.mobile.min.js"/>
		-->
	</xsl:template>
	
	<!-- Report controls-->
	<!-- ########################################################################################## -->
	<xsl:template name="controls">
		<div id="rprtControl">
			
			<fieldset id="controlgroupLOD" data-role="controlgroup" data-mini="true">
				<legend><xsl:value-of select="$lang/x:e[@key='LevelOfDetail']"/></legend>
				<input type="radio" name="radio-LOD" id="cntrlAllDetails" value="cntrlAllDetails"/>
				<label for="cntrlAllDetails"><xsl:value-of select="$lang/x:e[@key='AllDetails']"/></label>
				
				<input type="radio" name="radio-LOD" id="cntrlLessInformation"
					value="cntrlLessInformation"/>
				<label for="cntrlLessInformation"><xsl:value-of select="$lang/x:e[@key='LessInformation']"/></label>
				
				<input type="radio" name="radio-LOD" id="cntrlSimplified" value="cntrlSimplified"
					checked="checked"/>
				<label for="cntrlSimplified"><xsl:value-of select="$lang/x:e[@key='Simplified']"/></label>
			</fieldset>
			
			<fieldset id="controlgroupShow" data-role="controlgroup" data-mini="true">
				<legend><xsl:value-of select="$lang/x:e[@key='Show']"/></legend>
				<input type="radio" name="radio-Show" id="cntrlShowAlsoFailed"
					value="cntrlShowAlsoFailed" checked="checked"/>
				<label for="cntrlShowAlsoFailed"><xsl:value-of select="$lang/x:e[@key='All']"/></label>
				
				<input type="radio" name="radio-Show" id="cntrlShowOnlyFailed"
					value="cntrlShowOnlyFailed"/>
				<label for="cntrlShowOnlyFailed"><xsl:value-of select="$lang/x:e[@key='OnlyFailed']"/></label>
			</fieldset>
			
		</div>
	</xsl:template>
	
	<!-- Script tags in the footer-->
	<!-- ########################################################################################## -->
	<xsl:template name="footerScripts">
		<script>
			<!-- Controls for switching the level of detail -->
			$( "input[name=radio-Show]" ).on( "click",
			function() {
			var cntrl = $( "input[name=radio-Show]:checked" ).val();
			if(cntrl=="cntrlShowOnlyFailed")
			{
			$('.TestSuite').collapsible('expand');
			$( ".SuccessfulTestCase" ).hide('slow');
			$('.FailedTestCase').collapsible('expand');
			$('.SuccessfulTestStep').collapsible('collapse');
			$('.FailedTestStep').collapsible('expand');
			$( ".SuccessfulTestStep" ).hide('slow');
			$('.FailedAssertion').collapsible('expand');
			$('.SuccessfulAssertion').hide('slow');
			}
			else if(cntrl=="cntrlShowAlsoFailed")
			{
			$( ".SuccessfulTestCase" ).show('fast');
			$( ".SuccessfulTestStep" ).show('fast');
			$('.SuccessfulAssertion').show('fast');
			}
			});
			$( "input[name=radio-LOD]" ).on( "click",
			function() {
			var cntrl = $( "input[name=radio-LOD]:checked" ).val();
			if(cntrl=="cntrlSimplified")
			{
			$( ".ReportDetail" ).hide("slow");
			$('.DoNotShowInSimpleView').hide('slow');
			
			<!-- Cut text -->
			var assertionFailureMessage = 
			'Expected text value \'AssertionFailures:\' but was \'AssertionFailures:\'';
			$('.XQueryContainsAssertion').hide('slow');
			}
			else if(cntrl=="cntrlLessInformation")
			{
			$('.ReportDetail').hide('slow');
			$('.DoNotShowInSimpleView').show('slow');
			}
			else if(cntrl=="cntrlAllDetails")
			{
			$('.ReportDetail').show('slow');
			$('.DoNotShowInSimpleView').show('slow');
			}
			});
			
			$( document ).ready(function() {
			$( ".ReportDetail" ).hide();
			$('.DoNotShowInSimpleView').hide();
			});
			
		</script>
	</xsl:template>
	
</xsl:stylesheet>
