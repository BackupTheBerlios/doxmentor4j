var progressReporter = null;

function reindex()
{
   $('reindex').setStyle({display:'none'});
   $('reindexing').setStyle({display:'block'});
   $('searchbutton').setStyle({display:'none'});
   
   new Ajax.Request('/indexer', 
   					  {method: 'post',
   					  		onSuccess: indexingStarted,
								onFailure: indexingFailed,
								parameters: {start: 'y'} 
							});
}

function stopIndex()
{
   new Ajax.Request('/indexer',
   					  {method: 'post',
   					  		onSuccess: indexingStarted,
								onFailure: indexingFailed,
								parameters: {stop: 'y'}
							});
}

function indexingStarted(request)
{
   $('indexstatus').update(request.responseText);
   if (progressReporter == null)
	   progressReporter = 
	   	new Ajax.PeriodicalUpdater('indexstatus', '/indexer',
                                     {method: 'post',
                                     	onSuccess: indexingUpdate,
                                       frequency: 2,
                                       decay: 2,
                                       parameters: {update: 'y'} 
                                     });   
   else
   	progressReporter.start();
}

function indexingFailed(request)
{
	$('indexstatus').update("Indexing error on server");
   setTimeout("indexingStop()",5000)
}

function indexingUpdate(request)
{
	v = request.responseText;
   if (v) v = v.toLowerCase()
   if ( (v) && 
   		( (v.indexOf("complete.") >= 0) || (v.indexOf("already in progress") >= 0) ||
           (v.indexOf("errors occurred") >= 0) ) )
   {   
   	if (progressReporter != null)
         progressReporter.stop();
   	$('indexstatus').update(request.responseText);
   	setTimeout("indexingStop()",3000)
      return;
   }
}

function indexingStop()
{	
   $('indexstatus').update("Indexing:- Please be patient");
   $('reindexing').setStyle({display:'none'});
   $('reindex').setStyle({display:'block'});
   $('searchbutton').setStyle({display:'inline'});
   if (progressReporter != null)
      progressReporter.stop();
}
