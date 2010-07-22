function initTabs() 
{   
   var tabs = document.getElementsByClassName('tab');
   
	for (var i = 0; i < tabs.length; i++) 
   {
      if ($(tabs[i].id))
         $(tabs[i].id).onclick = function () 
         {
            tabSelected(this);
         }
   }
}

function tabSelected(li) 
{
//	alert(li + " " + li.id);
   if ( (li.id) && (li.id == 'current') )
      return;   
   var tabs = document.getElementsByClassName('tab');
   if (tabs == null) return;
   var newCurrent = null;
   for (var i = 0; i < tabs.length; i++) 
   {
      if ($(tabs[i].id).id == li.id)
         newCurrent = $(tabs[i].id);
      else
         $(tabs[i].id).id = 'tab' + (i+1);
   }
   if (newCurrent)
   {
      var as = newCurrent.getElementsByClassName("tablink");
      if ( (as) && (as.length > 0) ) 
         a = as[0];
      var href = null;
      if ( (a) && (a.href) )
      {
         var p = a.href.indexOf('#');         
         if ( (p++ >= 0) && (p < a.href.length) )
         	href = a.href.substring(p);
        	else
        		href = a.href;
      }
      newCurrent.id = 'current';
   }
   var activeDiv = null;
   if (href)
   	activeDiv = $(href);
   if (activeDiv)
   {
   	var divs = document.getElementsByClassName('tabcontent');
   	for (var i = 0; i < divs.length; i++) 	
   	{
   		var div = $(divs[i].id);
   		if (div)
   			div.setStyle({ display:'none' });
   	}
   	activeDiv.setStyle({ display:'block' });
   }
/*
   switch (id)
   {
      case 0:
         $('load').setStyle({ display:'block' });
         $('content').setStyle({ display:'none' });
         $('main').setStyle({ display:'block' });
         $('load').setStyle({ display:'none' });
         break;
         
      case 1:
         $('main').setStyle({ display:'none' });
         var searchText = $('searchtext').getValue();
         if (page <= 0) page = 1;
         var pg = "" + page;
         var searchReq = new Ajax.Request('/search', 
                                    {  method: 'post', 
                                       parameters: {  search: searchText,
                                                      page: pg 
                                                   }, 
                                       onLoading: showLoad, 
                                       onComplete: showResponse
                                    } );
         break;
   }
*/
}

function showLoad () 
{
	$('load').setStyle({ display:'block' });
   $('content').setStyle({ display:'none' });
}

function showResponse (originalRequest) 
{
	var newData = originalRequest.responseText;
	$('load').setStyle({ display:'none' });
	$('content').innerHTML = newData;
}
