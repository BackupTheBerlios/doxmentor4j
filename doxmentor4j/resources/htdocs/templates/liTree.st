$if(it.node)$
<li class="closed unloaded">
   $it.description$
   <ul>
      <li id="$it.path$">Loading...
   </ul>
</li>
$else$
   $if(it.leaf)$
      <li>
         $it.description$
         <ul>
            $it.views:templates/a()$
         </ul>
      </li>
   $else$
      <li>
            Invalid. Requires NODE or LEAF file
      </li>
   $endif$
$endif$