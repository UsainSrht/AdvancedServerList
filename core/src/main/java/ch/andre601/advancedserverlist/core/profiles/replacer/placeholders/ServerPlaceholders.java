/*
 * MIT License
 *
 * Copyright (c) 2022 Andre_601
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package ch.andre601.advancedserverlist.core.profiles.replacer.placeholders;

import ch.andre601.advancedserverlist.core.generics.GenericEventInfo;

import java.util.HashMap;
import java.util.Map;

public class ServerPlaceholders implements Placeholders{
    
    private final Map<String, Object> replacements = new HashMap<>();
    
    public ServerPlaceholders(GenericEventInfo info){
        this.replacements.put("${server playersOnline}", info.getPlayersOnline());
        this.replacements.put("${server playersMax}", info.getPlayersMax());
        
        if(info.getHost() != null)
            this.replacements.put("${server host}", info.getHost());
    }
    
    @Override
    public Map<String, Object> getReplacements(){
        return this.replacements;
    }
}