var searchRequest = null;

function search(searchText, page)
{
	if (arguments.length == 0)
   {
      searchText = $('searchtext').getValue();
      page = 1;
   }
	if ( (searchText) && (searchText.length > 0) )
	{
		if (page <= 0) page = 1;
		var pg = "" + page;
		searchRequest = new Ajax.Request('/search', 
                                    	{  method: 'post', 
                                       	parameters: {  search: searchText,
                                                      	page: pg 
                                                   	}, 
                                       	onLoading: showLoad, 
                                       	onComplete: showResponse
                                    	} );
	}
}

function showLoad () 
{
	$('load').setStyle({ display:'block' });   
}

function showResponse (originalRequest) 
{
	var newData = originalRequest.responseText;
	$('load').setStyle({ display:'none' });
	$('main').setStyle({ display:'none' });
	$('search').innerHTML = newData;
	if ($('tab2'))
		tabSelected($('tab2'));
	$('search').setStyle({ display:'block' });
}
